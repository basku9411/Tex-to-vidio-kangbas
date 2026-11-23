package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.AnimationFontOption
import com.example.model.EffectColorTheme
import com.example.model.PhotoAnimationConfig
import com.example.model.PhotoEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object EffectCanvasDrawer {
    private val rainbowPalette = intArrayOf(
        Color.parseColor("#9B8CFF"),
        Color.parseColor("#6CF2FF"),
        Color.parseColor("#FFD670"),
        Color.parseColor("#FF8BC8"),
        Color.parseColor("#8CFFB8"),
    )

    private val noteGlyphs = arrayOf("♪", "♫", "♬", "𝄞")
    private val leafColors = intArrayOf(
        Color.parseColor("#FF8A3D"),
        Color.parseColor("#FFC145"),
        Color.parseColor("#E85D3F"),
    )

    fun drawFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        photoBitmap: Bitmap?,
        config: PhotoAnimationConfig,
        timeSec: Float,
        fontTypefaces: Map<AnimationFontOption, Typeface> = emptyMap(),
        subtitleTypeface: Typeface? = null,
    ) {
        canvas.drawColor(Color.parseColor("#0B0D18"))
        val t = timeSec * config.speed.multiplier
        val density = config.density.multiplier
        val minDim = min(width, height)

        // 1. Background photo with optional Ken Burns smooth zoom/pan
        if (photoBitmap != null && !photoBitmap.isRecycled) {
            drawPhoto(
                canvas = canvas,
                width = width,
                height = height,
                bitmap = photoBitmap,
                kenBurns = PhotoEffect.KEN_BURNS in config.selectedEffects,
                timeSec = t,
            )
        }

        // 2. Light beams / God rays behind particles
        if (PhotoEffect.LIGHT_BEAMS in config.selectedEffects) {
            drawLightBeams(canvas, width, height, t, config.colorTheme)
        }

        // 2b. Drifting Clouds & Water Ripples (ambient backdrop layers)
        if (PhotoEffect.DRIFTING_CLOUDS in config.selectedEffects) {
            ExtraEffectsDrawer.drawDriftingClouds(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.WATER_RIPPLES in config.selectedEffects) {
            ExtraEffectsDrawer.drawWaterRipples(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }

        // 3. Bokeh soft glowing circles
        if (PhotoEffect.BOKEH in config.selectedEffects) {
            drawBokeh(canvas, width, height, minDim, t, density, config.colorTheme)
        }

        // 4. Rain
        if (PhotoEffect.RAIN in config.selectedEffects) {
            drawRain(canvas, width, height, minDim, t, density, config.colorTheme)
        }

        // 5. Snow
        if (PhotoEffect.SNOW in config.selectedEffects) {
            drawSnow(canvas, width, height, minDim, t, density, config.colorTheme)
        }

        // 6. Sakura / Autumn Leaves / Dandelion / Feathers
        if (PhotoEffect.SAKURA in config.selectedEffects) {
            drawPetals(canvas, width, height, minDim, t, density, isSakura = true)
        }
        if (PhotoEffect.AUTUMN_LEAVES in config.selectedEffects) {
            drawPetals(canvas, width, height, minDim, t, density, isSakura = false)
        }
        if (PhotoEffect.DANDELION_SEEDS in config.selectedEffects) {
            ExtraEffectsDrawer.drawDandelionSeeds(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.FLOATING_FEATHERS in config.selectedEffects) {
            ExtraEffectsDrawer.drawFloatingFeathers(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }

        // 7. Shooting stars & Crescent Moons
        if (PhotoEffect.SHOOTING_STARS in config.selectedEffects) {
            drawShootingStars(canvas, width, height, minDim, t, density, config.colorTheme)
        }
        if (PhotoEffect.CRESCENT_STARS in config.selectedEffects) {
            ExtraEffectsDrawer.drawCrescentStars(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }

        // 8. Fireflies, Gold Dust, Fairy Orbs & Sparkles
        if (PhotoEffect.GOLD_DUST in config.selectedEffects) {
            ExtraEffectsDrawer.drawGoldDust(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.FIREFLIES in config.selectedEffects) {
            drawFireflies(canvas, width, height, minDim, t, density, config.colorTheme)
        }
        if (PhotoEffect.FAIRY_ORBS in config.selectedEffects) {
            ExtraEffectsDrawer.drawFairyOrbs(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.SPARKLES in config.selectedEffects) {
            drawSparkles(canvas, width, height, minDim, t, density, config.colorTheme)
        }

        // 9. Lanterns & Confetti
        if (PhotoEffect.LANTERNS in config.selectedEffects) {
            drawLanterns(canvas, width, height, minDim, t, density)
        }
        if (PhotoEffect.CONFETTI in config.selectedEffects) {
            ExtraEffectsDrawer.drawConfetti(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }

        // 10. Animated Characters: Flying Birds, White Doves, Butterflies, Koi Fish & Dragonflies
        if (PhotoEffect.KOI_FISH in config.selectedEffects) {
            ExtraEffectsDrawer.drawKoiFish(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.FLYING_BIRDS in config.selectedEffects) {
            drawFlyingBirds(canvas, width, height, minDim, t, density, config.colorTheme)
        }
        if (PhotoEffect.DOVES_WHITE in config.selectedEffects) {
            ExtraEffectsDrawer.drawWhiteDoves(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }
        if (PhotoEffect.BUTTERFLIES in config.selectedEffects) {
            drawButterflies(canvas, width, height, minDim, t, density, config.colorTheme)
        }
        if (PhotoEffect.DRAGONFLIES in config.selectedEffects) {
            ExtraEffectsDrawer.drawDragonflies(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor, ::hash)
        }

        // 11. Floating Music Notes, Equalizer Bars & Hearts
        if (PhotoEffect.EQUALIZER_BARS in config.selectedEffects) {
            ExtraEffectsDrawer.drawEqualizerBars(canvas, width, height, minDim, t, density, config.colorTheme, ::pickThemeColor)
        }
        if (PhotoEffect.MUSIC_NOTES in config.selectedEffects) {
            drawMusicNotes(canvas, width, height, minDim, t, density, config.colorTheme)
        }
        if (PhotoEffect.HEARTS in config.selectedEffects) {
            drawHearts(canvas, width, height, minDim, t, density, config.colorTheme)
        }

        // 12. Glowing pulsing border frame
        if (PhotoEffect.GLOW_FRAME in config.selectedEffects) {
            drawGlowFrame(canvas, width, height, minDim, t, config.colorTheme)
        }

        // 13. Manual Text Overlays (razor-sharp on top)
        if (config.textOverlays.isNotEmpty()) {
            ExtraEffectsDrawer.drawTextOverlays(
                canvas = canvas,
                w = width,
                h = height,
                minDim = minDim,
                overlays = config.textOverlays,
                timeSec = timeSec,
                fontTypefaces = fontTypefaces,
            )
        }

        // 14. Timed AI Subtitles from Audio (when enabled in Animasi)
        if (config.timedSubtitles.isNotEmpty()) {
            val timeMs = (timeSec * 1000f).toLong()
            val activeSeg = config.timedSubtitles.firstOrNull { timeMs in it.startMs..it.endMs }
            if (activeSeg != null && activeSeg.text.isNotBlank()) {
                SubtitleCanvasDrawer.drawSubtitle(
                    canvas = canvas,
                    width = width,
                    height = height,
                    rawText = activeSeg.text,
                    preset = config.subtitlePreset,
                    position = config.subtitlePosition,
                    sizeOption = config.subtitleSize,
                    colorOverride = config.subtitleColor,
                    typeface = subtitleTypeface ?: Typeface.DEFAULT_BOLD,
                )
            }
        }
    }

    private fun drawPhoto(
        canvas: Canvas,
        width: Float,
        height: Float,
        bitmap: Bitmap,
        kenBurns: Boolean,
        timeSec: Float,
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val baseScale = maxOf(width / bw, height / bh)
        val zoom = if (kenBurns) {
            1.0f + 0.075f * (0.5f - 0.5f * cos(timeSec * 0.45f))
        } else {
            1.0f
        }
        val panX = if (kenBurns) sin(timeSec * 0.32f) * width * 0.025f else 0f
        val panY = if (kenBurns) cos(timeSec * 0.25f) * height * 0.02f else 0f

        val drawW = bw * baseScale * zoom
        val drawH = bh * baseScale * zoom
        val left = (width - drawW) * 0.5f + panX
        val top = (height - drawH) * 0.5f + panY

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
    }

    private fun drawMusicNotes(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (16 * density).toInt().coerceIn(6, 42)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until count) {
            val seed = hash(i * 31 + 7)
            val seed2 = hash(i * 47 + 13)
            val speed = 0.09f + seed * 0.08f
            val progress = ((t * speed + seed2) % 1.0f)
            val y = h * (1.08f - progress * 1.18f)
            val baseX = w * (0.06f + seed * 0.88f)
            val sway = sin(t * (1.5f + seed2) + i) * w * 0.04f
            val x = baseX + sway
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 235).toInt()
            val size = minDim * (0.045f + seed2 * 0.045f)
            val angle = sin(t * 2.1f + i) * 14f

            val baseColor = pickThemeColor(
                theme = theme,
                index = i,
                naturalColor = when (i % 3) {
                    0 -> Color.parseColor("#FFD670")
                    1 -> Color.parseColor("#9B8CFF")
                    else -> Color.parseColor("#6CF2FF")
                },
            )

            // Soft aura glow behind note
            glowPaint.shader = RadialGradient(
                x, y - size * 0.3f, size * 0.85f,
                Color.argb((alpha * 0.45f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y - size * 0.3f, size * 0.85f, glowPaint)

            textPaint.textSize = size
            textPaint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            textPaint.setShadowLayer(size * 0.2f, 0f, 2f, Color.argb((alpha * 0.7f).toInt(), 0, 0, 0))

            canvas.save()
            canvas.rotate(angle, x, y)
            canvas.drawText(noteGlyphs[i % noteGlyphs.size], x, y, textPaint)
            canvas.restore()
        }
    }

    private fun drawFlyingBirds(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (7 * density).toInt().coerceIn(3, 18)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()

        for (i in 0 until count) {
            val s1 = hash(i * 19 + 3)
            val s2 = hash(i * 53 + 11)
            val speed = 0.08f + s1 * 0.07f
            val progress = ((t * speed + s2) % 1.0f)
            val dir = if (i % 4 == 3) -1f else 1f
            val x = if (dir > 0) (-0.1f + progress * 1.2f) * w else (1.1f - progress * 1.2f) * w
            val y = h * (0.10f + s1 * 0.38f) + sin(t * 1.6f + i * 1.3f) * h * 0.035f
            val span = minDim * (0.042f + s2 * 0.032f)
            val flap = sin(t * (7.5f + s1 * 3f) + i * 2.1f)
            val wingTipY = -span * 0.45f * flap
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0.15f, 1f) * 235).toInt()

            val birdColor = pickThemeColor(theme, i, Color.parseColor("#F5F7FF"))
            strokePaint.color = Color.argb(alpha, Color.red(birdColor), Color.green(birdColor), Color.blue(birdColor))
            strokePaint.strokeWidth = (span * 0.13f).coerceAtLeast(2.2f)
            strokePaint.setShadowLayer(4f, 0f, 2f, Color.argb((alpha * 0.65f).toInt(), 0, 0, 0))

            path.reset()
            path.moveTo(x - span, y + wingTipY)
            path.quadTo(x - span * 0.45f, y - span * 0.32f * (0.5f + flap * 0.5f), x, y)
            path.quadTo(x + span * 0.45f, y - span * 0.32f * (0.5f + flap * 0.5f), x + span, y + wingTipY)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawFireflies(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (26 * density).toInt().coerceIn(10, 65)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until count) {
            val s1 = hash(i * 23 + 5)
            val s2 = hash(i * 41 + 9)
            val x = w * s1 + sin(t * (0.9f + s2) + i) * w * 0.08f
            val y = h * s2 + cos(t * (0.7f + s1) + i * 1.4f) * h * 0.08f
            val pulse = 0.25f + 0.75f * (0.5f + 0.5f * sin(t * (2.5f + s1 * 2f) + i * 1.7f))
            val radius = minDim * (0.014f + s2 * 0.016f)
            val color = pickThemeColor(theme, i, Color.parseColor("#FFD25A"))
            val alpha = (pulse * 220).toInt()

            glowPaint.shader = RadialGradient(
                x, y, radius * 2.3f,
                Color.argb((alpha * 0.7f).toInt(), Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius * 2.3f, glowPaint)
            corePaint.color = Color.argb(alpha, 255, 255, 235)
            canvas.drawCircle(x, y, radius * 0.4f, corePaint)
        }
    }

    private fun drawSparkles(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (22 * density).toInt().coerceIn(8, 52)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val path = Path()

        for (i in 0 until count) {
            val s1 = hash(i * 17 + 19)
            val s2 = hash(i * 37 + 29)
            val x = w * (0.05f + s1 * 0.9f)
            val y = h * (0.05f + s2 * 0.9f)
            val twinkle = ((sin(t * (2.6f + s1 * 2f) + i * 2.3f) + 1f) * 0.5f)
            val r = minDim * (0.012f + s2 * 0.018f) * (0.35f + 0.85f * twinkle)
            val alpha = (twinkle * 245).toInt()
            val color = pickThemeColor(theme, i, Color.WHITE)
            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

            val inner = r * 0.26f
            path.reset()
            path.moveTo(x, y - r)
            path.lineTo(x + inner, y - inner)
            path.lineTo(x + r, y)
            path.lineTo(x + inner, y + inner)
            path.lineTo(x, y + r)
            path.lineTo(x - inner, y + inner)
            path.lineTo(x - r, y)
            path.lineTo(x - inner, y - inner)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun drawRain(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (65 * density).toInt().coerceIn(25, 140)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }
        val color = pickThemeColor(theme, 0, Color.parseColor("#B8DCFF"))

        for (i in 0 until count) {
            val s1 = hash(i * 13 + 7)
            val s2 = hash(i * 29 + 3)
            val speed = 1.15f + s1 * 0.85f
            val progress = ((t * speed + s2) % 1.0f)
            val x = w * (-0.1f + s1 * 1.2f) - progress * w * 0.14f
            val y = h * (-0.15f + progress * 1.3f)
            val len = minDim * (0.055f + s2 * 0.05f)
            paint.strokeWidth = (1.4f + s1 * 1.6f)
            paint.color = Color.argb((110 + (s2 * 100).toInt()), Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawLine(x, y, x - len * 0.22f, y + len, paint)
        }
    }

    private fun drawSnow(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (45 * density).toInt().coerceIn(18, 100)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val color = pickThemeColor(theme, 0, Color.WHITE)

        for (i in 0 until count) {
            val s1 = hash(i * 43 + 1)
            val s2 = hash(i * 61 + 17)
            val progress = ((t * (0.12f + s1 * 0.12f) + s2) % 1.0f)
            val x = w * s1 + sin(t * (1.2f + s2) + i) * w * 0.05f
            val y = h * (-0.08f + progress * 1.16f)
            val r = minDim * (0.005f + s2 * 0.009f)
            val alpha = (140 + (s1 * 100).toInt()).coerceAtMost(245)
            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun drawPetals(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        isSakura: Boolean,
    ) {
        val count = (24 * density).toInt().coerceIn(10, 56)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        for (i in 0 until count) {
            val s1 = hash(i * 37 + if (isSakura) 5 else 21)
            val s2 = hash(i * 59 + if (isSakura) 11 else 31)
            val progress = ((t * (0.14f + s1 * 0.10f) + s2) % 1.0f)
            val x = ((w * s1 + progress * w * 0.25f + sin(t * 1.8f + i) * w * 0.04f) % (w * 1.1f)) - w * 0.05f
            val y = h * (-0.08f + progress * 1.16f)
            val size = minDim * (0.018f + s2 * 0.015f)
            val rot = t * (65f + s1 * 60f) + i * 35f

            val base = if (isSakura) {
                if (i % 2 == 0) Color.parseColor("#FFB3D1") else Color.parseColor("#FFD9E8")
            } else {
                leafColors[i % leafColors.size]
            }
            paint.color = Color.argb(215, Color.red(base), Color.green(base), Color.blue(base))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)
            rect.set(-size, -size * 0.52f, size, size * 0.52f)
            canvas.drawOval(rect, paint)
            canvas.restore()
        }
    }

    private fun drawBokeh(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (16 * density).toInt().coerceIn(6, 36)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        for (i in 0 until count) {
            val s1 = hash(i * 29 + 7)
            val s2 = hash(i * 71 + 13)
            val progress = ((t * (0.05f + s1 * 0.05f) + s2) % 1.0f)
            val x = w * s1 + sin(t * 0.7f + i) * w * 0.04f
            val y = h * (1.1f - progress * 1.25f)
            val r = minDim * (0.035f + s2 * 0.06f)
            val color = pickThemeColor(theme, i, rainbowPalette[i % rainbowPalette.size])
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 85).toInt()
            fillPaint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            ringPaint.color = Color.argb((alpha * 1.3f).toInt().coerceAtMost(140), Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawCircle(x, y, r, fillPaint)
            canvas.drawCircle(x, y, r, ringPaint)
        }
    }

    private fun drawHearts(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (14 * density).toInt().coerceIn(6, 34)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()

        for (i in 0 until count) {
            val s1 = hash(i * 41 + 9)
            val s2 = hash(i * 67 + 23)
            val progress = ((t * (0.10f + s1 * 0.07f) + s2) % 1.0f)
            val x = w * (0.08f + s1 * 0.84f) + sin(t * 1.7f + i) * w * 0.04f
            val y = h * (1.08f - progress * 1.18f)
            val size = minDim * (0.025f + s2 * 0.022f)
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 225).toInt()
            val color = pickThemeColor(theme, i, if (i % 2 == 0) Color.parseColor("#FF7BA9") else Color.parseColor("#FFB5CE"))
            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

            path.reset()
            path.moveTo(x, y + size * 0.35f)
            path.cubicTo(x - size, y - size * 0.35f, x - size * 0.55f, y - size * 1.05f, x, y - size * 0.35f)
            path.cubicTo(x + size * 0.55f, y - size * 1.05f, x + size, y - size * 0.35f, x, y + size * 0.35f)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun drawButterflies(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (8 * density).toInt().coerceIn(3, 18)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val wingRect = RectF()

        for (i in 0 until count) {
            val s1 = hash(i * 47 + 17)
            val s2 = hash(i * 83 + 5)
            val progress = ((t * (0.08f + s1 * 0.06f) + s2) % 1.0f)
            val x = w * (-0.08f + progress * 1.16f)
            val y = h * (0.18f + s1 * 0.62f) + sin(t * 2.2f + i * 1.5f) * h * 0.06f
            val size = minDim * (0.022f + s2 * 0.015f)
            val flapScale = 0.35f + 0.65f * kotlin.math.abs(sin(t * 8.5f + i))
            val color = pickThemeColor(theme, i, rainbowPalette[i % rainbowPalette.size])
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0.2f, 1f) * 235).toInt()
            paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(-15f + sin(t * 2f + i) * 18f)
            wingRect.set(-size * flapScale, -size * 0.7f, 0f, size * 0.55f)
            canvas.drawOval(wingRect, paint)
            wingRect.set(0f, -size * 0.7f, size * flapScale, size * 0.55f)
            canvas.drawOval(wingRect, paint)
            canvas.restore()
        }
    }

    private fun drawLanterns(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
    ) {
        val count = (10 * density).toInt().coerceIn(4, 24)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        for (i in 0 until count) {
            val s1 = hash(i * 53 + 11)
            val s2 = hash(i * 79 + 23)
            val progress = ((t * (0.065f + s1 * 0.045f) + s2) % 1.0f)
            val x = w * (0.08f + s1 * 0.84f) + sin(t * 1.1f + i) * w * 0.03f
            val y = h * (1.1f - progress * 1.2f)
            val size = minDim * (0.024f + s2 * 0.022f)
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 235).toInt()

            glowPaint.shader = RadialGradient(
                x, y, size * 2.0f,
                Color.argb((alpha * 0.55f).toInt(), 255, 195, 85),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, size * 2.0f, glowPaint)

            bodyPaint.color = Color.argb(alpha, 255, 165, 65)
            rect.set(x - size * 0.55f, y - size * 0.8f, x + size * 0.55f, y + size * 0.8f)
            canvas.drawRoundRect(rect, size * 0.35f, size * 0.35f, bodyPaint)
            bodyPaint.color = Color.argb(alpha, 255, 240, 175)
            canvas.drawCircle(x, y + size * 0.45f, size * 0.25f, bodyPaint)
        }
    }

    private fun drawShootingStars(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
    ) {
        val count = (4 * density).toInt().coerceIn(2, 10)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until count) {
            val period = 3.2f + hash(i * 17) * 2.5f
            val phase = (t + i * 1.4f) % period
            if (phase > 1.1f) continue
            val p = phase / 1.1f
            val startX = w * (0.25f + hash(i * 31) * 0.75f)
            val startY = h * (0.03f + hash(i * 43) * 0.35f)
            val headX = startX - p * w * 0.55f
            val headY = startY + p * h * 0.35f
            val tailLen = minDim * 0.18f
            val tailX = headX + tailLen
            val tailY = headY - tailLen * (h * 0.35f / (w * 0.55f))
            val color = pickThemeColor(theme, i, Color.parseColor("#E8EEFF"))
            val alpha = ((1f - p) * 240).toInt().coerceIn(0, 240)
            paint.strokeWidth = 4f
            paint.shader = LinearGradient(
                headX, headY, tailX, tailY,
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawLine(headX, headY, tailX, tailY, paint)
        }
    }

    private fun drawLightBeams(
        canvas: Canvas,
        w: Float,
        h: Float,
        t: Float,
        theme: EffectColorTheme,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val color = pickThemeColor(theme, 0, Color.parseColor("#A79BFF"))
        val sweep = (sin(t * 0.65f) * 0.5f + 0.5f)
        val cx = w * (0.2f + sweep * 0.6f)
        paint.shader = RadialGradient(
            cx, -h * 0.1f, maxOf(w, h) * 0.95f,
            Color.argb(65, Color.red(color), Color.green(color), Color.blue(color)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun drawGlowFrame(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        theme: EffectColorTheme,
    ) {
        val pulse = 0.5f + 0.5f * sin(t * 2.2f)
        val c1 = pickThemeColor(theme, 0, Color.parseColor("#9B8CFF"))
        val c2 = pickThemeColor(theme, 1, Color.parseColor("#FFD670"))
        val pad = minDim * 0.025f
        val radius = minDim * 0.04f
        val stroke = minDim * (0.008f + 0.005f * pulse)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = LinearGradient(
                0f, 0f, w, h,
                Color.argb((150 + 95 * pulse).toInt(), Color.red(c1), Color.green(c1), Color.blue(c1)),
                Color.argb((150 + 95 * pulse).toInt(), Color.red(c2), Color.green(c2), Color.blue(c2)),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(RectF(pad, pad, w - pad, h - pad), radius, radius, paint)
    }

    private fun pickThemeColor(theme: EffectColorTheme, index: Int, naturalColor: Int): Int {
        return when (theme) {
            EffectColorTheme.NATURAL -> naturalColor
            EffectColorTheme.GOLD -> if (index % 2 == 0) Color.parseColor("#FFD670") else Color.parseColor("#FFA94D")
            EffectColorTheme.COSMIC -> if (index % 2 == 0) Color.parseColor("#9B8CFF") else Color.parseColor("#6CF2FF")
            EffectColorTheme.SNOW_WHITE -> Color.WHITE
            EffectColorTheme.RAINBOW -> rainbowPalette[index % rainbowPalette.size]
        }
    }

    private fun hash(n: Int): Float {
        var x = n * 374761393 + 668265263
        x = (x xor (x ushr 13)) * 1274126177
        x = x xor (x ushr 16)
        return ((x and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat())
    }
}
