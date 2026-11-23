package com.example.model

import androidx.annotation.FontRes
import com.example.R

enum class PhotoEffectCategory(val label: String) {
    CHARACTERS("Karakter & Hewan"),
    MUSICAL("Musik & Cahaya"),
    NATURE("Alam & Musim"),
    WEATHER("Cuaca & Langit"),
    CAMERA("Kamera & Bingkai"),
}

enum class PhotoEffect(
    val label: String,
    val emoji: String,
    val category: PhotoEffectCategory,
) {
    // Karakter & Hewan
    FLYING_BIRDS("Burung Terbang", "🐦", PhotoEffectCategory.CHARACTERS),
    DOVES_WHITE("Merpati Putih", "🕊️", PhotoEffectCategory.CHARACTERS),
    BUTTERFLIES("Kupu-kupu Terbang", "🦋", PhotoEffectCategory.CHARACTERS),
    KOI_FISH("Ikan Koi Berenang", "🐟", PhotoEffectCategory.CHARACTERS),
    DRAGONFLIES("Capung Berkilaun", "🪁", PhotoEffectCategory.CHARACTERS),
    FLOATING_FEATHERS("Bulu Sayap Halus", "🪶", PhotoEffectCategory.CHARACTERS),

    // Musik & Cahaya
    MUSIC_NOTES("Not Doremi Melayang", "🎵", PhotoEffectCategory.MUSICAL),
    EQUALIZER_BARS("Gelombang Musik", "🎼", PhotoEffectCategory.MUSICAL),
    FIREFLIES("Kunang-kunang Cahaya", "✨", PhotoEffectCategory.MUSICAL),
    FAIRY_ORBS("Bola Peri Cahaya", "🔮", PhotoEffectCategory.MUSICAL),
    SPARKLES("Bintang Berkilau", "⭐", PhotoEffectCategory.MUSICAL),
    HEARTS("Hati Melayang", "💖", PhotoEffectCategory.MUSICAL),
    LANTERNS("Lentera Terbang", "🏮", PhotoEffectCategory.MUSICAL),
    CONFETTI("Konfeti Pesta", "🎉", PhotoEffectCategory.MUSICAL),

    // Alam & Musim
    SAKURA("Kelopak Sakura", "🌸", PhotoEffectCategory.NATURE),
    AUTUMN_LEAVES("Daun Gugur", "🍂", PhotoEffectCategory.NATURE),
    DANDELION_SEEDS("Benih Dandelion", "🌾", PhotoEffectCategory.NATURE),
    GOLD_DUST("Debu Emas Mewah", "🪄", PhotoEffectCategory.NATURE),

    // Cuaca & Langit
    RAIN("Hujan Rintik", "🌧️", PhotoEffectCategory.WEATHER),
    SNOW("Salju Turun", "❄️", PhotoEffectCategory.WEATHER),
    BOKEH("Gelembung Bokeh", "🫧", PhotoEffectCategory.WEATHER),
    SHOOTING_STARS("Bintang Jatuh", "🌠", PhotoEffectCategory.WEATHER),
    CRESCENT_STARS("Bulan & Bintang", "🌙", PhotoEffectCategory.WEATHER),
    DRIFTING_CLOUDS("Awan Berjalan", "☁️", PhotoEffectCategory.WEATHER),
    WATER_RIPPLES("Riak Air Berkilau", "💧", PhotoEffectCategory.WEATHER),

    // Kamera & Bingkai
    LIGHT_BEAMS("Sinar Cahaya", "🌤️", PhotoEffectCategory.CAMERA),
    GLOW_FRAME("Bingkai Berdenyut", "🖼️", PhotoEffectCategory.CAMERA),
    KEN_BURNS("Zoom Kamera Halus", "🎥", PhotoEffectCategory.CAMERA),
}

enum class EffectDensity(val label: String, val multiplier: Float) {
    LOW("Sedikit", 0.55f),
    MEDIUM("Sedang", 1.0f),
    HIGH("Ramai", 1.7f),
}

enum class EffectSpeed(val label: String, val multiplier: Float) {
    SLOW("Pelan", 0.6f),
    NORMAL("Normal", 1.0f),
    FAST("Cepat", 1.65f),
}

enum class EffectColorTheme(val label: String) {
    NATURAL("Asli"),
    GOLD("Emas Hangat"),
    COSMIC("Ungu Kosmik"),
    SNOW_WHITE("Putih Bersih"),
    RAINBOW("Pelangi"),
}

enum class PhotoVideoRatio(
    val label: String,
    val width: Int,
    val height: Int,
) {
    LANDSCAPE_16_9("Landscape 16:9", 1280, 720),
    PORTRAIT_9_16("Portrait 9:16", 720, 1280),
    SQUARE_1_1("Kotak 1:1", 720, 720),
}

enum class PhotoVideoDuration(val seconds: Int, val label: String) {
    D5(5, "5 dtk"),
    D10(10, "10 dtk"),
    D15(15, "15 dtk"),
    D30(30, "30 dtk"),
    D60(60, "60 dtk"),
}

enum class AnimationFontOption(
    val label: String,
    @FontRes val fontRes: Int,
    val cssFamily: String,
) {
    PLUS_JAKARTA("Jakarta Modern", R.font.plus_jakarta_sans, "\"Plus Jakarta Sans\", sans-serif"),
    PLAYFAIR("Playfair Elegan", R.font.playfair_display, "\"Playfair Display\", serif"),
    BEBAS_NEUE("Bebas Tegas", R.font.bebas_neue, "\"Bebas Neue\", sans-serif"),
    CINZEL("Cinzel Klasik", R.font.cinzel, "\"Cinzel\", serif"),
    PACIFICO("Pacifico Latin", R.font.pacifico, "\"Pacifico\", cursive"),
    LOBSTER("Lobster Retro", R.font.lobster, "\"Lobster\", cursive"),
    CAVEAT("Caveat Tangan", R.font.caveat, "\"Caveat\", cursive"),
    BANGERS("Bangers Komik", R.font.bangers, "\"Bangers\", cursive"),
    RIGHTEOUS("Righteous Poster", R.font.righteous, "\"Righteous\", sans-serif"),
    ORBITRON("Orbitron Neon", R.font.orbitron, "\"Orbitron\", sans-serif"),
    FREDOKA("Fredoka Bulat", R.font.fredoka, "\"Fredoka\", sans-serif"),
    AMIRI("Amiri Kaligrafi", R.font.amiri, "\"Amiri\", serif"),
    SPACE_GROTESK("Space Minimal", R.font.space_grotesk, "\"Space Grotesk\", sans-serif"),
    JETBRAINS_MONO("Mono Kode", R.font.jetbrains_mono, "\"JetBrains Mono\", monospace"),
}

enum class AnimationTextStyle(val label: String) {
    OUTLINE("Garis Tepi"),
    CAPSULE("Kapsul Gelap"),
    NEON("Cahaya Neon"),
    CLEAN_SHADOW("Bayangan Halus"),
    GOLD_BANNER("Pita Emas"),
}

enum class AnimationTextMotion(val label: String) {
    STATIC("Diam"),
    FLOAT("Melayang"),
    PULSE("Berdenyut"),
    FADE_IN("Muncul Halus"),
    TYPEWRITER("Ketik"),
}

enum class AnimationTextColor(val label: String, val hex: String) {
    WHITE("Putih", "#FFFFFF"),
    GOLD("Emas", "#FFD670"),
    YELLOW("Kuning", "#FFE55C"),
    CYAN("Cyan", "#6CF2FF"),
    PINK("Pink", "#FF8BC8"),
    VIOLET("Ungu", "#C8B8FF"),
    MINT("Mint", "#8CFFB8"),
    CORAL("Koral", "#FF7B6B"),
}

data class PhotoTextOverlay(
    val id: String,
    val text: String,
    val font: AnimationFontOption = AnimationFontOption.PLAYFAIR,
    val sizeFraction: Float = 0.068f,
    val yFraction: Float = 0.80f,
    val color: AnimationTextColor = AnimationTextColor.WHITE,
    val style: AnimationTextStyle = AnimationTextStyle.OUTLINE,
    val motion: AnimationTextMotion = AnimationTextMotion.FLOAT,
)

data class PhotoAnimationConfig(
    val selectedEffects: Set<PhotoEffect> = setOf(
        PhotoEffect.MUSIC_NOTES,
        PhotoEffect.FIREFLIES,
    ),
    val density: EffectDensity = EffectDensity.MEDIUM,
    val speed: EffectSpeed = EffectSpeed.NORMAL,
    val colorTheme: EffectColorTheme = EffectColorTheme.NATURAL,
    val ratio: PhotoVideoRatio = PhotoVideoRatio.LANDSCAPE_16_9,
    val durationSeconds: Float = 10f,
    val durationSourceLabel: String? = null,
    val textOverlays: List<PhotoTextOverlay> = emptyList(),
    val timedSubtitles: List<SubtitleSegment> = emptyList(),
    val subtitlePreset: SubtitleStylePreset = SubtitleStylePreset.TIKTOK_POP,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val subtitleSize: SubtitleSizeOption = SubtitleSizeOption.MEDIUM,
    val subtitleColor: SubtitleColorOverride = SubtitleColorOverride.PRESET,
)
