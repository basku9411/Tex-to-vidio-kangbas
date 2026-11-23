package com.example.model

import androidx.annotation.FontRes
import com.example.R

data class SubtitleSegment(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

enum class SubtitleStylePreset(
    val label: String,
    val previewSample: String,
    @FontRes val fontRes: Int,
    val uppercase: Boolean,
    val textColorHex: Long,
    val strokeColorHex: Long,
    val strokeWidthRatio: Float,
    val boxColorHex: Long?,
    val shadowColorHex: Long,
    val glowRadius: Float = 0f,
) {
    TIKTOK_POP(
        label = "TikTok Pop",
        previewSample = "TEKS TEBAL POP",
        fontRes = R.font.bebas_neue,
        uppercase = true,
        textColorHex = 0xFFFFFFFF,
        strokeColorHex = 0xFF000000,
        strokeWidthRatio = 0.14f,
        boxColorHex = null,
        shadowColorHex = 0xCC000000,
    ),
    CONTRAST_BOX(
        label = "Kotak Kontras",
        previewSample = "Kapsul Gelap Jelas",
        fontRes = R.font.plus_jakarta_sans,
        uppercase = false,
        textColorHex = 0xFFFFFFFF,
        strokeColorHex = 0x00000000,
        strokeWidthRatio = 0f,
        boxColorHex = 0xD90B0E1A,
        shadowColorHex = 0x66000000,
    ),
    CINEMATIC_GOLD(
        label = "Emas Sinematik",
        previewSample = "Klasik Emas Elegan",
        fontRes = R.font.cinzel,
        uppercase = true,
        textColorHex = 0xFFFFD670,
        strokeColorHex = 0xFF2B1C00,
        strokeWidthRatio = 0.08f,
        boxColorHex = null,
        shadowColorHex = 0xEE000000,
    ),
    NEON_GLOW(
        label = "Neon Glow",
        previewSample = "CAHAYA NEON UNGU",
        fontRes = R.font.orbitron,
        uppercase = true,
        textColorHex = 0xFFF2ECFF,
        strokeColorHex = 0xFF6B5CE7,
        strokeWidthRatio = 0.09f,
        boxColorHex = 0x66161136,
        shadowColorHex = 0xFF9B8CFF,
        glowRadius = 18f,
    ),
    COMIC_PLAYFUL(
        label = "Ceria / Komik",
        previewSample = "Gaya Bulat Kuning!",
        fontRes = R.font.fredoka,
        uppercase = false,
        textColorHex = 0xFFFFEB3B,
        strokeColorHex = 0xFF1A1200,
        strokeWidthRatio = 0.15f,
        boxColorHex = null,
        shadowColorHex = 0xCC000000,
    ),
    ESTHETIC_SCRIPT(
        label = "Estetik Script",
        previewSample = "Tulisan Tangan Lembut",
        fontRes = R.font.pacifico,
        uppercase = false,
        textColorHex = 0xFFFFF8E7,
        strokeColorHex = 0xFF241738,
        strokeWidthRatio = 0.08f,
        boxColorHex = null,
        shadowColorHex = 0xDD000000,
    ),
    VIOLET_BADGE(
        label = "Sorot Ungu",
        previewSample = "Kartu Ungu Modern",
        fontRes = R.font.space_grotesk,
        uppercase = false,
        textColorHex = 0xFFFFFFFF,
        strokeColorHex = 0x00000000,
        strokeWidthRatio = 0f,
        boxColorHex = 0xE65947CC,
        shadowColorHex = 0x99000000,
    ),
    MINIMAL_CLEAN(
        label = "Minimalis Bening",
        previewSample = "Subtitel Bersih Film",
        fontRes = R.font.plus_jakarta_sans,
        uppercase = false,
        textColorHex = 0xFFF5F7FF,
        strokeColorHex = 0x66000000,
        strokeWidthRatio = 0.05f,
        boxColorHex = null,
        shadowColorHex = 0xEE000000,
    ),
}

enum class SubtitlePosition(val label: String, val verticalCenterFraction: Float) {
    BOTTOM("Bawah", 0.84f),
    CENTER("Tengah", 0.50f),
    TOP("Atas", 0.16f),
}

enum class SubtitleSizeOption(val label: String, val widthFractionFontSize: Float) {
    SMALL("Kecil", 0.042f),
    MEDIUM("Sedang", 0.056f),
    LARGE("Besar", 0.072f),
}

enum class SubtitleColorOverride(val label: String, val colorHex: Long?) {
    PRESET("Bawaan Gaya", null),
    WHITE("Putih", 0xFFFFFFFF),
    YELLOW("Kuning Cerah", 0xFFFFE55C),
    CYAN("Biru Cyan", 0xFF6CF2FF),
    PINK("Merah Muda", 0xFFFF8BC8),
}
