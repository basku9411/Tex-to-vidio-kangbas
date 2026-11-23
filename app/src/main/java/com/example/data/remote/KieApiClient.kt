package com.example.data.remote

import com.example.model.AiModel
import com.example.model.ApiKeyBalance
import com.example.model.CreditCalculator
import com.example.model.CreditPoolSummary
import com.example.model.ResolutionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class KieApiException(
    message: String,
    val statusCode: Int = 0,
) : Exception(message) {
    fun isAuthFailure(): Boolean {
        if (statusCode == 401 || statusCode == 403) return true
        return Regex(
            "invalid api.?key|api.?key.*(invalid|expired|disabled)|unauthorized|forbidden",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(message ?: "")
    }

    fun isInsufficientCredit(): Boolean {
        if (statusCode == 402) return true
        return Regex(
            "insufficient.?credit|credit.?not.?enough|not enough credit|insufficient balance|balance insufficient|kredit tidak cukup|saldo tidak cukup",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(message ?: "")
    }

    fun isRetryableWithAnotherKey(): Boolean = isAuthFailure() || isInsufficientCredit()
}

data class TaskPollResult(
    val state: String,
    val progress: String,
    val videoUrl: String?,
    val errorMessage: String?,
)

class KieApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    companion object {
        private const val API_BASE = "https://api.kie.ai"
        private const val UPLOAD_URL = "https://kieai.redpandaai.co/api/file-stream-upload"
    }

    suspend fun fetchKeyCredit(apiKey: String): ApiKeyBalance = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_BASE/api/v1/chat/credit")
                .get()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()

            val (code, bodyStr) = executeRaw(request)
            val json = runCatching { JSONObject(bodyStr) }.getOrNull()
                ?: throw KieApiException("Balasan Kie.ai tidak terbaca.", code)

            val apiCode = if (json.has("code") && !json.isNull("code")) json.optInt("code", code) else 200
            if (code !in 200..299 || apiCode != 200 || json.optBoolean("success", true).not()) {
                val msg = extractErrorMessage(json, code)
                throw KieApiException(msg, code)
            }

            val amount = extractCreditAmount(json)
                ?: throw KieApiException(extractErrorMessage(json, code), code)

            ApiKeyBalance(key = apiKey, balance = amount, error = null)
        } catch (e: Exception) {
            ApiKeyBalance(
                key = apiKey,
                balance = null,
                error = e.message ?: "Gagal memeriksa saldo",
            )
        }
    }

    /**
     * Queries every API key in [keys] and sums all valid credit balances across all keys.
     */
    suspend fun fetchAllKeyCredits(keys: List<String>): CreditPoolSummary = coroutineScope {
        if (keys.isEmpty()) {
            return@coroutineScope CreditPoolSummary(
                totalCredits = null,
                keyBalances = emptyList(),
                failedMessages = emptyList(),
            )
        }
        val results = keys.map { key ->
            async { fetchKeyCredit(key) }
        }.awaitAll()

        val failedLines = results.mapIndexedNotNull { index, item ->
            if (item.balance == null) {
                "Kunci ${index + 1}: gagal (${item.error ?: "kesalahan tidak diketahui"})"
            } else {
                null
            }
        }
        CreditPoolSummary(
            totalCredits = CreditCalculator.sumValidCredits(results),
            keyBalances = results,
            failedMessages = failedLines,
        )
    }

    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        apiKey: String,
    ): String = withContext(Dispatchers.IO) {
        val mediaType = mimeType.toMediaType()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mediaType))
            .addFormDataPart("uploadPath", "images/user-uploads")
            .addFormDataPart("fileName", fileName)
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val json = executeJson(request)
        val downloadUrl = json.optJSONObject("data")?.optString("downloadUrl")?.takeIf { it.isNotBlank() }
            ?: throw KieApiException(json.optString("msg").ifBlank { "Gagal mengunggah foto." })
        downloadUrl
    }

    suspend fun createTask(
        model: AiModel,
        input: JSONObject,
        apiKey: String,
    ): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("model", model.modelId)
            put("input", input)
        }
        val request = Request.Builder()
            .url("$API_BASE/api/v1/jobs/createTask")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        val json = executeJson(request)
        val taskId = json.optJSONObject("data")?.optString("taskId")?.takeIf { it.isNotBlank() }
            ?: throw KieApiException("Kie.ai tidak mengembalikan Task ID.")
        taskId
    }

    suspend fun getRecordInfo(
        taskId: String,
        apiKey: String,
    ): TaskPollResult = withContext(Dispatchers.IO) {
        val url = "$API_BASE/api/v1/jobs/recordInfo".toHttpUrl().newBuilder()
            .addQueryParameter("taskId", taskId)
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val json = executeJson(request)
        val d = json.optJSONObject("data") ?: JSONObject()
        val state = (d.optString("state").ifBlank { d.optString("status") }).lowercase()
        val progress = d.optString("progress", "")
        val videoUrl = extractVideoUrl(d)
        val errorMessage = d.optString("failMsg").ifBlank { d.optString("errorMessage") }.ifBlank { null }
        TaskPollResult(
            state = state,
            progress = progress,
            videoUrl = videoUrl,
            errorMessage = errorMessage,
        )
    }

    fun buildTaskInput(
        model: AiModel,
        prompt: String,
        aspectRatio: String,
        resolution: ResolutionOption,
        durationSeconds: Int,
        audio: Boolean,
        imageUrl: String?,
    ): JSONObject {
        val input = JSONObject().apply {
            put("prompt", prompt.trim())
        }
        val res = resolution.wireValue
        when (model) {
            AiModel.SEEDANCE_V1_LITE_TEXT,
            AiModel.SEEDANCE_V1_LITE_IMAGE -> {
                if (!imageUrl.isNullOrBlank()) {
                    input.put("image_url", imageUrl)
                } else if (model == AiModel.SEEDANCE_V1_LITE_IMAGE) {
                    throw KieApiException("Model Seedance V1 Lite Image membutuhkan foto.")
                }
                input.put("aspect_ratio", aspectRatio)
                input.put("resolution", if (res == "1080p") "720p" else res)
                input.put("duration", (if (durationSeconds >= 10) 10 else 5).toString())
                input.put("camera_fixed", false)
            }
            AiModel.SEEDANCE_2_MINI -> {
                if (!imageUrl.isNullOrBlank()) {
                    input.put("reference_image_urls", JSONArray().put(imageUrl))
                }
                input.put("generate_audio", audio)
                input.put("resolution", if (res == "1080p") "720p" else res)
                input.put("aspect_ratio", aspectRatio)
                input.put("duration", durationSeconds.coerceIn(4, 15))
                input.put("web_search", false)
            }
            AiModel.LTX_FAST_TEXT,
            AiModel.LTX_FAST_IMAGE -> {
                if (!imageUrl.isNullOrBlank()) {
                    input.put("image_url", imageUrl)
                } else if (model == AiModel.LTX_FAST_IMAGE) {
                    throw KieApiException("Model LTX Image membutuhkan foto.")
                }
                input.put("resolution", if (res == "720p") "720p" else "1080p")
                input.put("duration", CreditCalculator.nearestLtxDuration(durationSeconds))
                input.put("fps", 24)
            }
            AiModel.SEEDANCE_2_5,
            AiModel.SEEDANCE_2_5_FLASH -> {
                if (!imageUrl.isNullOrBlank()) {
                    input.put("reference_image_urls", JSONArray().put(imageUrl))
                }
                input.put("generate_audio", audio)
                input.put("resolution", res)
                input.put("aspect_ratio", aspectRatio)
                input.put("duration", durationSeconds)
                input.put("return_last_frame", false)
            }
            AiModel.VEO_3_1 -> {
                if (!imageUrl.isNullOrBlank()) {
                    input.put("image_urls", JSONArray().put(imageUrl))
                    input.put("generation_type", "FIRST_AND_LAST_FRAMES_2_VIDEO")
                } else {
                    input.put("generation_type", "TEXT_2_VIDEO")
                }
                input.put("aspect_ratio", aspectRatio)
                input.put("enable_translation", true)
                input.put("model", if (res == "1080p") "veo3" else "veo3_fast")
            }
            AiModel.KLING_2_6_TEXT -> {
                if (!imageUrl.isNullOrBlank()) {
                    throw KieApiException("Model Kling 2.6 Text hanya untuk Text to Video.")
                }
                input.put("sound", audio)
                input.put("aspect_ratio", aspectRatio)
                input.put("duration", (if (durationSeconds >= 10) 10 else 5).toString())
            }
            AiModel.KLING_2_6_IMAGE -> {
                if (imageUrl.isNullOrBlank()) {
                    throw KieApiException("Model Kling 2.6 Image membutuhkan foto. Pilih Image to Video.")
                }
                input.put("image_urls", JSONArray().put(imageUrl))
                input.put("sound", audio)
                input.put("duration", (if (durationSeconds >= 10) 10 else 5).toString())
            }
        }
        return input
    }

    private suspend fun executeJson(request: Request): JSONObject {
        val (statusCode, bodyStr) = executeRaw(request)
        val json = runCatching { JSONObject(bodyStr) }.getOrNull()
            ?: throw KieApiException("Balasan Kie.ai tidak terbaca.", statusCode)

        val apiCode = if (json.has("code") && !json.isNull("code")) json.optInt("code", 200) else 200
        val success = if (json.has("success")) json.optBoolean("success", true) else true
        if (statusCode !in 200..299 || apiCode != 200 || !success) {
            throw KieApiException(extractErrorMessage(json, statusCode), statusCode)
        }
        return json
    }

    private suspend fun executeRaw(request: Request): Pair<Int, String> =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            KieApiException(e.message ?: "Gagal terhubung ke server Kie.ai.")
                        )
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        val bodyStr = res.body?.string().orEmpty()
                        if (cont.isActive) {
                            cont.resume(res.code to bodyStr)
                        }
                    }
                }
            })
        }

    private fun extractCreditAmount(json: JSONObject): Double? {
        val dataRaw = json.opt("data")
        return when (dataRaw) {
            is Number -> dataRaw.toDouble().takeIf { it.isFinite() }
            is JSONObject -> {
                val keys = listOf("credit", "balance", "available")
                keys.firstNotNullOfOrNull { k ->
                    if (dataRaw.has(k) && !dataRaw.isNull(k)) {
                        dataRaw.optDouble(k).takeIf { it.isFinite() }
                    } else null
                }
            }
            else -> null
        }
    }

    private fun extractErrorMessage(json: JSONObject, statusCode: Int): String {
        return json.optString("msg")
            .ifBlank { json.optString("message") }
            .ifBlank { json.optString("error") }
            .ifBlank { "HTTP $statusCode" }
    }

    private fun extractVideoUrl(data: JSONObject): String? {
        data.optString("videoUrl").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
        data.optJSONArray("resultUrls")?.optString(0)?.takeIf { it.isNotBlank() && it != "null" }?.let { return it }

        val rawObj = when (val raw = data.opt("resultJson")) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        }
        if (rawObj != null) {
            rawObj.optJSONArray("resultUrls")?.optString(0)?.takeIf { it.isNotBlank() && it != "null" }?.let { return it }
            rawObj.optString("video_url").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
            rawObj.optString("videoUrl").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
            rawObj.optString("url").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
        }
        return null
    }
}
