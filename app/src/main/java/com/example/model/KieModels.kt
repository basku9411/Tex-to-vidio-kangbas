package com.example.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class AppTab {
    CREATE,
    PHOTO_FX,
    LIBRARY,
    SETTINGS,
}

enum class AnimationStudioMode {
    PHOTO_ANIMATION,
    AUTO_CAPTION,
}

enum class GenerationMode(val wireValue: String) {
    TEXT("text"),
    IMAGE("image"),
}

enum class ModelGroup(val label: String) {
    ECONOMY("Hemat / Ekonomis"),
    HIGH_QUALITY("Kualitas tinggi"),
}

enum class AiModel(
    val key: String,
    val modelId: String,
    val label: String,
    val hint: String,
    val group: ModelGroup,
    val requiresImage: Boolean = false,
    val textOnly: Boolean = false,
    val supports480p: Boolean = false,
) {
    SEEDANCE_V1_LITE_TEXT(
        key = "seedanceV1LiteText",
        modelId = "bytedance/v1-lite-text-to-video",
        label = "Seedance V1 Lite · Text",
        hint = "Ekonomis · Text to Video · 480p/720p",
        group = ModelGroup.ECONOMY,
        textOnly = true,
        supports480p = true,
    ),
    SEEDANCE_V1_LITE_IMAGE(
        key = "seedanceV1LiteImage",
        modelId = "bytedance/v1-lite-image-to-video",
        label = "Seedance V1 Lite · Image",
        hint = "Ekonomis · Image to Video · 480p/720p",
        group = ModelGroup.ECONOMY,
        requiresImage = true,
        supports480p = true,
    ),
    SEEDANCE_2_MINI(
        key = "seedance2mini",
        modelId = "bytedance/seedance-2-mini",
        label = "Seedance 2.0 Mini · Text / Image",
        hint = "Ekonomis · Text dan gambar ke video · 480p/720p",
        group = ModelGroup.ECONOMY,
        supports480p = true,
    ),
    LTX_FAST_TEXT(
        key = "ltxFastText",
        modelId = "ltx/2-5-fast-text-to-video",
        label = "LTX 2.5 Fast · Text",
        hint = "Text to Video · audio asli · 720p–1440p",
        group = ModelGroup.ECONOMY,
        textOnly = true,
    ),
    LTX_FAST_IMAGE(
        key = "ltxFastImage",
        modelId = "ltx/2-5-fast-image-to-video",
        label = "LTX 2.5 Fast · Image",
        hint = "Image to Video · audio asli · 720p–1440p",
        group = ModelGroup.ECONOMY,
        requiresImage = true,
    ),
    SEEDANCE_2_5(
        key = "seedance25",
        modelId = "bytedance/seedance-2-5",
        label = "Seedance 2.5",
        hint = "Text dan gambar ke video · Audio opsional",
        group = ModelGroup.HIGH_QUALITY,
    ),
    SEEDANCE_2_5_FLASH(
        key = "seedance25flash",
        modelId = "bytedance/seedance-2-5-flash",
        label = "Seedance 2.5 Flash",
        hint = "Versi Flash · Text dan gambar ke video",
        group = ModelGroup.HIGH_QUALITY,
    ),
    VEO_3_1(
        key = "veo31",
        modelId = "veo-3-1",
        label = "Veo 3.1",
        hint = "Text dan gambar ke video · Fast / Quality",
        group = ModelGroup.HIGH_QUALITY,
    ),
    KLING_2_6_TEXT(
        key = "kling26text",
        modelId = "kling-2.6/text-to-video",
        label = "Kling 2.6 · Text",
        hint = "Kling 2.6 · Text to Video",
        group = ModelGroup.HIGH_QUALITY,
        textOnly = true,
    ),
    KLING_2_6_IMAGE(
        key = "kling26image",
        modelId = "kling-2.6/image-to-video",
        label = "Kling 2.6 · Image",
        hint = "Kling 2.6 · Image to Video",
        group = ModelGroup.HIGH_QUALITY,
        requiresImage = true,
    ),
}

enum class AspectRatioOption(val wireValue: String, val label: String) {
    LANDSCAPE("16:9", "Landscape 16:9"),
    PORTRAIT("9:16", "Portrait 9:16"),
    SQUARE("1:1", "Kotak 1:1"),
}

enum class ResolutionOption(val wireValue: String, val label: String) {
    R480("480p", "480p · Hemat"),
    R720("720p", "720p"),
    R1080("1080p", "1080p"),
}

enum class DurationOption(val seconds: Int, val label: String) {
    D5(5, "5 detik"),
    D10(10, "10 detik"),
    D15(15, "15 detik"),
}

enum class VideoStatus(val wireValue: String) {
    QUEUED("queued"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        fun fromWire(value: String): VideoStatus =
            entries.firstOrNull { it.wireValue == value } ?: FAILED
    }
}

data class PriceEstimate(
    val credits: Double?,
    val detail: String,
)

data class ApiKeyBalance(
    val key: String,
    val balance: Double?,
    val error: String? = null,
)

data class CreditPoolSummary(
    val totalCredits: Double?,
    val keyBalances: List<ApiKeyBalance>,
    val failedMessages: List<String>,
)

data class NoticeMessage(
    val icon: String,
    val title: String,
    val description: String,
)

object CreditCalculator {
    private val ltxDurations = listOf(6, 8, 10, 12, 14, 16, 18, 20)

    fun nearestLtxDuration(seconds: Int): Int {
        return ltxDurations.reduce { best, n ->
            if (abs(n - seconds) < abs(best - seconds)) n else best
        }
    }

    fun estimateCredits(
        model: AiModel,
        durationSeconds: Int,
        resolution: ResolutionOption,
        audio: Boolean,
    ): Double? {
        val res = resolution.wireValue
        return when (model) {
            AiModel.SEEDANCE_V1_LITE_TEXT,
            AiModel.SEEDANCE_V1_LITE_IMAGE -> {
                val rate = if (res == "480p") 2.0 else 4.5
                val dur = if (durationSeconds >= 10) 10 else 5
                rate * dur
            }
            AiModel.SEEDANCE_2_MINI -> {
                val rate = if (res == "480p") 3.8 else 8.2
                rate * durationSeconds.coerceIn(4, 15)
            }
            AiModel.LTX_FAST_TEXT,
            AiModel.LTX_FAST_IMAGE -> {
                val rate = if (res == "1080p") 15.6 else 10.8
                rate * nearestLtxDuration(durationSeconds)
            }
            AiModel.SEEDANCE_2_5 -> {
                val rate = if (res == "1080p") 158.0 else 63.0
                rate * durationSeconds
            }
            AiModel.SEEDANCE_2_5_FLASH -> {
                if (res == "1080p") null else 41.0 * durationSeconds
            }
            AiModel.KLING_2_6_TEXT,
            AiModel.KLING_2_6_IMAGE -> {
                val base = if (durationSeconds >= 10) 126.0 else 63.0
                base * (if (audio) 2 else 1)
            }
            AiModel.VEO_3_1 -> {
                if (res == "1080p") 255.0 else 60.0
            }
        }
    }

    fun buildPriceEstimate(
        model: AiModel,
        durationSeconds: Int,
        resolution: ResolutionOption,
        audio: Boolean,
    ): PriceEstimate {
        val cost = estimateCredits(model, durationSeconds, resolution, audio)
            ?: return PriceEstimate(
                credits = null,
                detail = "Resolusi/durasi ini belum memiliki harga terverifikasi pada kartu harga Kie.ai. Pilih pengaturan lain.",
            )

        val res = resolution.wireValue
        val detail = when (model) {
            AiModel.SEEDANCE_V1_LITE_TEXT,
            AiModel.SEEDANCE_V1_LITE_IMAGE -> {
                val rateStr = if (res == "480p") "2" else "4,5"
                val dur = if (durationSeconds >= 10) 10 else 5
                "Seedance V1 Lite · $res · $rateStr kredit/detik × $dur detik."
            }
            AiModel.SEEDANCE_2_MINI -> {
                val rateStr = if (res == "480p") "3,8" else "8,2"
                "Seedance 2.0 Mini · $res · $rateStr kredit/detik tanpa referensi video."
            }
            AiModel.LTX_FAST_TEXT,
            AiModel.LTX_FAST_IMAGE -> {
                val rateStr = if (res == "1080p") "15,6" else "10,8"
                "LTX 2.5 Fast · $res · $rateStr kredit/detik × $durationSeconds detik."
            }
            AiModel.SEEDANCE_2_5 -> {
                val rate = if (res == "1080p") 158 else 63
                "Seedance 2.5 · $res · $rate kredit/detik × $durationSeconds detik."
            }
            AiModel.SEEDANCE_2_5_FLASH -> {
                "Seedance 2.5 Flash · 720p · 41 kredit/detik × $durationSeconds detik."
            }
            AiModel.KLING_2_6_TEXT,
            AiModel.KLING_2_6_IMAGE -> {
                val durStr = if (durationSeconds >= 10) "10" else "5"
                val audioStr = if (audio) "audio aktif (biaya 2×)" else "tanpa audio"
                "Kling 2.6 · $durStr detik · $audioStr."
            }
            AiModel.VEO_3_1 -> {
                "Veo 3.1 Fast 720p / Quality 1080p · biaya per video."
            }
        }
        return PriceEstimate(credits = cost, detail = detail)
    }

    fun formatIdNumber(value: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return nf.format(value)
    }

    fun sumValidCredits(balances: List<ApiKeyBalance>): Double? {
        val valid = balances.mapNotNull { it.balance }.filter { it.isFinite() }
        return if (valid.isEmpty()) null else valid.sumOf { it.coerceAtLeast(0.0) }
    }
}
