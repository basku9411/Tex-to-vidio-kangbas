package com.example.data.remote

import android.util.Log
import com.example.model.SubtitleSegment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AutoCaptionAiRepository {
    companion object {
        private const val TAG = "AutoCaptionAiRepo"
    }

    private val subtitleSchema = Schema.obj(
        properties = mapOf(
            "segments" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startMs" to Schema.integer(description = "Start time of the spoken phrase in milliseconds"),
                        "endMs" to Schema.integer(description = "End time of the spoken phrase in milliseconds"),
                        "text" to Schema.string(description = "Transcribed short subtitle text (2 to 7 words)"),
                    )
                ),
                description = "Chronological subtitle segments matching the speech/lyrics in the audio",
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = subtitleSchema
            },
        )
    }

    suspend fun transcribeAudio(
        mediaBytes: ByteArray,
        mimeType: String,
        videoDurationMs: Long,
    ): List<SubtitleSegment> {
        val prompt = buildString {
            appendLine("Transkripsikan seluruh ucapan, dialog, narasi, atau lirik lagu dari audio video ini dengan akurat dalam bahasa aslinya (contoh: Bahasa Indonesia, Jawa, Sunda, Arab, atau Inggris sesuai suara yang terdengar).")
            appendLine("Pecah menjadi potongan subtitle pendek yang enak dibaca di video (maksimal 2-6 kata per segmen).")
            appendLine("Setiap segmen wajib memiliki waktu startMs dan endMs yang tepat sesuai audio, berada antara 0 sampai $videoDurationMs milidetik, dengan endMs > startMs.")
            appendLine("Jangan menambahkan teks yang tidak ada di audio. Jika audio hanya musik instrumental tanpa vokal sama sekali, kembalikan array segments kosong [].")
        }

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(mediaBytes, mimeType)
                        text(prompt)
                    }
                )
            }

            val jsonText = response.text
                ?: throw IllegalStateException("Model AI mengembalikan respons kosong.")
            val root = JSONObject(jsonText)
            val segmentsArr = root.optJSONArray("segments")
                ?: return emptyList()

            val parsed = mutableListOf<SubtitleSegment>()
            for (i in 0 until segmentsArr.length()) {
                val obj = segmentsArr.optJSONObject(i) ?: continue
                val text = obj.optString("text", "").trim()
                if (text.isBlank()) continue
                val rawStart = obj.optLong("startMs", 0L).coerceIn(0L, videoDurationMs)
                val rawEnd = obj.optLong("endMs", rawStart + 1500L)
                    .coerceIn(rawStart + 250L, videoDurationMs.coerceAtLeast(rawStart + 250L))
                parsed.add(
                    SubtitleSegment(
                        id = "seg-${i + 1}-${System.currentTimeMillis() % 10000}",
                        startMs = rawStart,
                        endMs = rawEnd,
                        text = text,
                    )
                )
            }
            return parsed.sortedBy { it.startMs }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            throw IllegalStateException("Transkripsi AI melebihi batas waktu 60 detik. Coba video yang lebih pendek.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transcribe video audio", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Gagal menganalisis audio video dengan AI."
            )
        }
    }
}
