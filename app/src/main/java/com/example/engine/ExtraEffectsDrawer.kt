package com.example.engine

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
import com.example.model.AnimationTextMotion
import com.example.model.AnimationTextStyle
import com.example.model.EffectColorTheme
import com.example.model.PhotoTextOverlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ExtraEffectsDrawer {

    private val confettiColors = intArrayOf(
        Color.parseColor("#FF6B9D"),
        Color.parseColor("#FFD670"),
        Color.parseColor("#6CF2FF"),
        Color.parseColor("#9B8CFF"),
        Color.parseColor("#8CFFB8"),
        Color.parseColor("#FFA94D"),
    )

    fun drawWhiteDoves(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (5 * density).toInt().coerceIn(2, 12)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 3f, Color.argb(120, 0, 0, 0))
        }
        val path = Path()

        for (i in 0 until count) {
            val s1 = hash(i * 31 + 19)
            val s2 = hash(i * 67 + 29)
            val progress = (t * (0.06f + s1 * 0.045f) + s2) % 1f
            val dir = if (i % 3 == 2) -1f else 1f
            val x = if (dir > 0) (-0.12f + progress * 1.24f) * w else (1.12f - progress * 1.24f) * w
            val y = h * (0.14f + s1 * 0.45f) + sin(t * 1.4f + i * 1.8f) * h * 0.045f
            val size = minDim * (0.042f + s2 * 0.025f)
            val flap = sin(t * (5.6f + s1 * 2f) + i * 2.1f)
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0.15f, 1f) * 240).toInt()
            val doveColor = colorPicker(theme, i, Color.parseColor("#FFFFFF"))
            val bodyCol = Color.argb(alpha, Color.red(doveColor), Color.green(doveColor), Color.blue(doveColor))

            canvas.save()
            canvas.translate(x, y)
            canvas.scale(dir, 1f)
            canvas.rotate(-8f + sin(t * 1.4f + i) * 7f)

            // Tail fan
            fillPaint.color = bodyCol
            path.reset()
            path.moveTo(-size * 0.45f, size * 0.04f)
            path.lineTo(-size * 1.05f, -size * 0.22f)
            path.lineTo(-size * 0.95f, size * 0.26f)
            path.close()
            canvas.drawPath(path, fillPaint)

            // Dove torso & head
            canvas.drawOval(RectF(-size * 0.55f, -size * 0.16f, size * 0.45f, size * 0.20f), fillPaint)
            canvas.drawCircle(size * 0.48f, -size * 0.12f, size * 0.16f, fillPaint)

            // Golden beak
            fillPaint.color = Color.argb(alpha, 255, 198, 92)
            path.reset()
            path.moveTo(size * 0.61f, -size * 0.15f)
            path.lineTo(size * 0.78f, -size * 0.08f)
            path.lineTo(size * 0.61f, -size * 0.03f)
            path.close()
            canvas.drawPath(path, fillPaint)

            // Sculpted flapping wings
            wingPaint.color = bodyCol
            val wingLift = -size * (0.65f + 0.45f * flap)
            path.reset()
            path.moveTo(-size * 0.12f, 0f)
            path.quadTo(-size * 0.35f, wingLift * 1.15f, -size * 0.85f, wingLift)
            path.quadTo(-size * 0.28f, wingLift * 0.45f, size * 0.22f, -size * 0.06f)
            path.close()
            canvas.drawPath(path, wingPaint)

            // Back secondary wing when downstroke
            if (flap < 0.3f) {
                wingPaint.color = Color.argb((alpha * 0.78f).toInt(), Color.red(doveColor), Color.green(doveColor), Color.blue(doveColor))
                path.reset()
                path.moveTo(-size * 0.05f, -size * 0.04f)
                path.quadTo(size * 0.05f, wingLift * 0.82f, -size * 0.42f, wingLift * 0.78f)
                path.quadTo(-size * 0.12f, wingLift * 0.3f, size * 0.20f, -size * 0.04f)
                path.close()
                canvas.drawPath(path, wingPaint)
            }
            canvas.restore()
        }
    }

    fun drawKoiFish(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (5 * density).toInt().coerceIn(2, 11)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val path = Path()

        for (i in 0 until count) {
            val s1 = hash(i * 43 + 7)
            val s2 = hash(i * 71 + 13)
            val progress = (t * (0.055f + s1 * 0.04f) + s2) % 1f
            val dir = if (i % 2 == 0) 1f else -1f
            val x = if (dir > 0) (-0.12f + progress * 1.24f) * w else (1.12f - progress * 1.24f) * w
            val y = h * (0.25f + s1 * 0.58f) + sin(t * 1.3f + i * 1.9f) * h * 0.05f
            val sz = minDim * (0.036f + s2 * 0.022f)
            val wiggle = sin(t * 5.2f + i * 1.7f) * sz * 0.28f
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0.18f, 1f) * 225).toInt()
            val primary = colorPicker(theme, i, if (i % 2 == 0) Color.parseColor("#FF7A45") else Color.parseColor("#FFF4E6"))

            canvas.save()
            canvas.translate(x, y)
            canvas.scale(dir, 1f)

            // Tail fin
            paint.color = Color.argb((alpha * 0.72f).toInt(), Color.red(primary), Color.green(primary), Color.blue(primary))
            path.reset()
            path.moveTo(-sz * 0.65f, wiggle * 0.45f)
            path.lineTo(-sz * 1.35f, wiggle - sz * 0.34f)
            path.quadTo(-sz * 1.1f, wiggle, -sz * 1.35f, wiggle + sz * 0.34f)
            path.close()
            canvas.drawPath(path, paint)

            // Sleek curved body
            paint.color = Color.argb(alpha, Color.red(primary), Color.green(primary), Color.blue(primary))
            path.reset()
            path.moveTo(sz * 0.72f, 0f)
            path.quadTo(sz * 0.15f, -sz * 0.36f, -sz * 0.68f, wiggle * 0.45f)
            path.quadTo(sz * 0.15f, sz * 0.36f, sz * 0.72f, 0f)
            path.close()
            canvas.drawPath(path, paint)

            // Koi spots
            spotPaint.color = Color.argb(
                alpha,
                if (i % 2 == 0) 255 else 240,
                if (i % 2 == 0) 248 else 88,
                if (i % 2 == 0) 235 else 48,
            )
            canvas.drawCircle(sz * 0.12f, -sz * 0.04f, sz * 0.16f, spotPaint)
            canvas.drawCircle(-sz * 0.22f, wiggle * 0.2f + sz * 0.04f, sz * 0.12f, spotPaint)
            canvas.restore()
        }
    }

    fun drawDragonflies(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (6 * density).toInt().coerceIn(3, 14)
        val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
        }

        for (i in 0 until count) {
            val s1 = hash(i * 37 + 11)
            val s2 = hash(i * 59 + 31)
            val x = w * (0.1f + s1 * 0.8f) + sin(t * (1.2f + s2 * 0.8f) + i * 2.1f) * w * 0.16f
            val y = h * (0.15f + s2 * 0.7f) + cos(t * (1.7f + s1 * 0.7f) + i * 1.4f) * h * 0.12f
            val sz = minDim * (0.022f + s2 * 0.014f)
            val flutter = sin(t * 28f + i * 3f) * 14f
            val col = colorPicker(theme, i, Color.parseColor("#6CF2FF"))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(sin(t * 2.4f + i) * 28f)

            // 4 translucent shimmering wings
            wingPaint.color = Color.argb(145, 225, 248, 255)
            for (side in listOf(-1f, 1f)) {
                canvas.save()
                canvas.rotate(side * (26f + flutter))
                canvas.drawOval(RectF(-sz * 0.22f, -sz * 1.1f, sz * 0.22f, 0f), wingPaint)
                canvas.rotate(side * 22f)
                canvas.drawOval(RectF(-sz * 0.18f, -sz * 0.9f, sz * 0.18f, 0f), wingPaint)
                canvas.restore()
            }

            // Iridescent slender tail/body
            bodyPaint.color = Color.argb(235, Color.red(col), Color.green(col), Color.blue(col))
            bodyPaint.strokeWidth = max(2.2f, sz * 0.18f)
            canvas.drawLine(-sz * 0.8f, 0f, sz * 0.45f, 0f, bodyPaint)
            canvas.restore()
        }
    }

    fun drawFloatingFeathers(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (14 * density).toInt().coerceIn(5, 32)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val quillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

        for (i in 0 until count) {
            val s1 = hash(i * 29 + 13)
            val s2 = hash(i * 61 + 5)
            val progress = (t * (0.075f + s1 * 0.05f) + s2) % 1f
            val x = w * (0.06f + s1 * 0.88f) + sin(t * 1.35f + i * 1.9f) * w * 0.07f
            val y = h * (-0.08f + progress * 1.18f)
            val sz = minDim * (0.028f + s2 * 0.022f)
            val rot = sin(t * 1.5f + i) * 32f + i * 24f
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 215).toInt()
            val col = colorPicker(theme, i, if (i % 2 == 0) Color.parseColor("#FFFDF9") else Color.parseColor("#E3DCFF"))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)
            paint.color = Color.argb((alpha * 0.72f).toInt(), Color.red(col), Color.green(col), Color.blue(col))
            canvas.drawOval(RectF(-sz, -sz * 0.28f, sz, sz * 0.28f), paint)
            quillPaint.color = Color.argb(alpha, 255, 255, 255)
            quillPaint.strokeWidth = max(1.5f, sz * 0.08f)
            canvas.drawLine(-sz * 1.15f, 0f, sz * 0.9f, 0f, quillPaint)
            canvas.restore()
        }
    }

    fun drawDandelionSeeds(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (18 * density).toInt().coerceIn(8, 42)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

        for (i in 0 until count) {
            val s1 = hash(i * 47 + 3)
            val s2 = hash(i * 83 + 17)
            val progress = (t * (0.065f + s1 * 0.05f) + s2) % 1f
            val x = ((w * (-0.08f + s1 * 0.9f + progress * 0.45f)) % (w * 1.1f))
            val y = h * (0.95f - progress * 0.9f) + sin(t * 1.4f + i) * h * 0.04f
            val sz = minDim * (0.018f + s2 * 0.016f)
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 220).toInt()
            val col = colorPicker(theme, i, Color.parseColor("#FFF8EB"))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(sin(t * 1.2f + i) * 20f)
            linePaint.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
            linePaint.strokeWidth = max(1.2f, sz * 0.07f)
            canvas.drawLine(0f, sz * 0.85f, 0f, -sz * 0.1f, linePaint)
            for (k in -3..3) {
                val a = (-90 + k * 22) * PI.toFloat() / 180f
                canvas.drawLine(0f, -sz * 0.1f, cos(a) * sz * 0.85f, -sz * 0.1f + sin(a) * sz * 0.85f, linePaint)
            }
            canvas.restore()
        }
    }

    fun drawCrescentStars(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val moons = (4 * density).toInt().coerceIn(2, 9)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()

        for (i in 0 until moons) {
            val s1 = hash(i * 31 + 23)
            val s2 = hash(i * 73 + 11)
            val x = w * (0.10f + s1 * 0.80f) + sin(t * 0.55f + i) * w * 0.025f
            val y = h * (0.10f + s2 * 0.42f) + cos(t * 0.65f + i) * h * 0.02f
            val r = minDim * (0.030f + s2 * 0.026f)
            val pulse = 0.65f + 0.35f * sin(t * 1.8f + i * 1.5f)
            val alpha = (210 * pulse).toInt()
            val col = colorPicker(theme, i, Color.parseColor("#FFE484"))

            glow.shader = RadialGradient(
                x, y, r * 2.1f,
                Color.argb((alpha * 0.42f).toInt(), Color.red(col), Color.green(col), Color.blue(col)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 2.1f, glow)

            val outer = Path().apply { addCircle(x, y, r, Path.Direction.CW) }
            val cutout = Path().apply { addCircle(x + r * 0.42f, y - r * 0.14f, r * 0.82f, Path.Direction.CW) }
            path.op(outer, cutout, Path.Op.DIFFERENCE)
            paint.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
            canvas.drawPath(path, paint)

            // Companion star
            val sx = x + r * 1.1f
            val sy = y - r * 0.35f
            val sr = r * 0.25f * (0.7f + 0.35f * sin(t * 3.5f + i))
            canvas.drawCircle(sx, sy, sr, paint)
        }
    }

    fun drawFairyOrbs(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (14 * density).toInt().coerceIn(6, 32)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        for (i in 0 until count) {
            val s1 = hash(i * 53 + 7)
            val s2 = hash(i * 97 + 13)
            val angle = t * (0.7f + s1 * 0.6f) + i * 1.7f
            val cx = w * (0.12f + s1 * 0.76f)
            val cy = h * (0.14f + s2 * 0.72f)
            val x = cx + cos(angle) * minDim * 0.10f
            val y = cy + sin(angle * 1.3f) * minDim * 0.08f
            val r = minDim * (0.015f + s2 * 0.018f)
            val col = colorPicker(theme, i, if (i % 2 == 0) Color.parseColor("#9B8CFF") else Color.parseColor("#6CF2FF"))
            val alpha = (150 + 85 * sin(t * 2.5f + i)).toInt().coerceIn(60, 235)

            glow.shader = RadialGradient(
                x, y, r * 2.6f,
                Color.argb((alpha * 0.75f).toInt(), Color.red(col), Color.green(col), Color.blue(col)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 2.6f, glow)
            ring.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(x, y, r * 0.65f, ring)
        }
    }

    fun drawConfetti(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (42 * density).toInt().coerceIn(16, 95)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        for (i in 0 until count) {
            val s1 = hash(i * 23 + 17)
            val s2 = hash(i * 53 + 29)
            val progress = (t * (0.20f + s1 * 0.14f) + s2) % 1f
            val x = w * s1 + sin(t * 2.4f + i) * w * 0.045f
            val y = h * (-0.08f + progress * 1.18f)
            val sz = minDim * (0.012f + s2 * 0.012f)
            val col = colorPicker(theme, i, confettiColors[i % confettiColors.size])
            paint.color = Color.argb(230, Color.red(col), Color.green(col), Color.blue(col))

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(t * (140f + s1 * 90f) + i * 27f)
            val flipY = 0.35f + 0.65f * abs(cos(t * 4.5f + i))
            rect.set(-sz, -sz * 0.45f * flipY, sz, sz * 0.45f * flipY)
            canvas.drawRoundRect(rect, 3f, 3f, paint)
            canvas.restore()
        }
    }

    fun drawDriftingClouds(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (6 * density).toInt().coerceIn(3, 12)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until count) {
            val s1 = hash(i * 19 + 41)
            val s2 = hash(i * 43 + 7)
            val progress = (t * (0.032f + s1 * 0.025f) + s2) % 1f
            val x = w * (-0.25f + progress * 1.5f)
            val y = h * (0.08f + s1 * 0.28f)
            val sz = minDim * (0.09f + s2 * 0.07f)
            val col = colorPicker(theme, i, Color.parseColor("#FFFFFF"))
            val alpha = (sin(progress * PI.toFloat()).coerceIn(0f, 1f) * 80).toInt()
            paint.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))

            canvas.drawCircle(x, y, sz * 0.62f, paint)
            canvas.drawCircle(x - sz * 0.55f, y + sz * 0.14f, sz * 0.46f, paint)
            canvas.drawCircle(x + sz * 0.55f, y + sz * 0.12f, sz * 0.48f, paint)
        }
    }

    fun drawWaterRipples(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (8 * density).toInt().coerceIn(3, 18)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        for (i in 0 until count) {
            val s1 = hash(i * 37 + 19)
            val s2 = hash(i * 67 + 41)
            val progress = (t * (0.35f + s1 * 0.2f) + s2) % 1f
            val x = w * (0.12f + s1 * 0.76f)
            val y = h * (0.20f + s2 * 0.68f)
            val maxR = minDim * (0.08f + s2 * 0.08f)
            val r = maxR * progress
            val alpha = ((1f - progress) * 160).toInt()
            val col = colorPicker(theme, i, Color.parseColor("#90E6FF"))
            paint.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
            paint.strokeWidth = max(1.5f, (1f - progress) * 3.5f)
            canvas.drawOval(RectF(x - r, y - r * 0.42f, x + r, y + r * 0.42f), paint)
        }
    }

    fun drawGoldDust(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
        hash: (Int) -> Float,
    ) {
        val count = (52 * density).toInt().coerceIn(20, 120)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until count) {
            val s1 = hash(i * 17 + 43)
            val s2 = hash(i * 61 + 29)
            val p = (t * (0.04f + s1 * 0.05f) + s2) % 1f
            val x = w * s1 + sin(t * 0.9f + i * 0.8f) * w * 0.05f
            val y = h * (1.04f - p * 1.12f)
            val r = minDim * (0.0025f + s2 * 0.0055f)
            val shimmer = 0.35f + 0.65f * abs(sin(t * 3.2f + i * 1.7f))
            val col = colorPicker(theme, i, if (i % 2 == 0) Color.parseColor("#FFD670") else Color.parseColor("#FFF2B8"))
            paint.color = Color.argb((shimmer * 225).toInt(), Color.red(col), Color.green(col), Color.blue(col))
            canvas.drawCircle(x, y, r, paint)
        }
    }

    fun drawEqualizerBars(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        t: Float,
        density: Float,
        theme: EffectColorTheme,
        colorPicker: (EffectColorTheme, Int, Int) -> Int,
    ) {
        val barCount = (20 * density).toInt().coerceIn(10, 36)
        val totalSpan = w * 0.82f
        val startX = (w - totalSpan) * 0.5f
        val slotW = totalSpan / barCount
        val barW = slotW * 0.62f
        val baseY = h * 0.955f
        val maxH = minDim * 0.14f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        for (i in 0 until barCount) {
            val wave = 0.22f + 0.78f * abs(sin(t * 5.2f + i * 0.55f) * cos(t * 2.3f - i * 0.3f))
            val barH = max(minDim * 0.015f, maxH * wave)
            val x = startX + i * slotW + (slotW - barW) * 0.5f
            val c1 = colorPicker(theme, i, Color.parseColor("#6CF2FF"))
            val c2 = colorPicker(theme, i + 1, Color.parseColor("#9B8CFF"))
            paint.shader = LinearGradient(
                x, baseY - barH, x, baseY,
                Color.argb(215, Color.red(c1), Color.green(c1), Color.blue(c1)),
                Color.argb(190, Color.red(c2), Color.green(c2), Color.blue(c2)),
                Shader.TileMode.CLAMP,
            )
            rect.set(x, baseY - barH, x + barW, baseY)
            canvas.drawRoundRect(rect, barW * 0.45f, barW * 0.45f, paint)
        }
    }

    fun drawTextOverlays(
        canvas: Canvas,
        w: Float,
        h: Float,
        minDim: Float,
        overlays: List<PhotoTextOverlay>,
        timeSec: Float,
        fontTypefaces: Map<AnimationFontOption, Typeface>,
    ) {
        if (overlays.isEmpty() || w <= 0f || h <= 0f) return

        overlays.forEachIndexed { idx, overlay ->
            val raw = overlay.text.trim()
            if (raw.isEmpty()) return@forEachIndexed

            val displayText = when (overlay.motion) {
                AnimationTextMotion.TYPEWRITER -> {
                    val chars = max(1, ((timeSec * 14f) % (raw.length + 12f)).toInt().coerceAtMost(raw.length))
                    raw.take(chars)
                }
                else -> raw
            }

            val baseAlpha = when (overlay.motion) {
                AnimationTextMotion.FADE_IN -> ((timeSec / 1.4f).coerceIn(0f, 1f))
                else -> 1f
            }
            val scaleFactor = when (overlay.motion) {
                AnimationTextMotion.PULSE -> 1f + 0.055f * sin(timeSec * 3.1f + idx)
                else -> 1f
            }
            val floatDy = when (overlay.motion) {
                AnimationTextMotion.FLOAT -> sin(timeSec * 1.9f + idx * 1.2f) * minDim * 0.014f
                else -> 0f
            }

            val fontSize = (minDim * overlay.sizeFraction).coerceIn(14f, minDim * 0.16f)
            val typeface = fontTypefaces[overlay.font] ?: Typeface.DEFAULT_BOLD
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = fontSize
                textAlign = Paint.Align.CENTER
            }

            val lines = wrapText(displayText, fillPaint, w * 0.86f)
            val lineHeight = fontSize * 1.20f
            val blockHeight = lines.size * lineHeight
            var widest = 24f
            lines.forEach { widest = max(widest, fillPaint.measureText(it)) }

            val centerY = h * overlay.yFraction + floatDy
            val top = (centerY - blockHeight * 0.5f).coerceIn(h * 0.06f, h * 0.94f - blockHeight)
            val alphaInt = (baseAlpha * 255).toInt().coerceIn(0, 255)
            val textCol = Color.parseColor(overlay.color.hex)

            canvas.save()
            canvas.scale(scaleFactor, scaleFactor, w * 0.5f, top + blockHeight * 0.5f)

            // Optional background box / banner
            val padX = fontSize * 0.55f
            val padY = fontSize * 0.28f
            val boxRect = RectF(
                w * 0.5f - widest * 0.5f - padX,
                top - padY,
                w * 0.5f + widest * 0.5f + padX,
                top + blockHeight + padY,
            )

            when (overlay.style) {
                AnimationTextStyle.CAPSULE -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((alphaInt * 0.78f).toInt(), 11, 14, 28)
                    }
                    canvas.drawRoundRect(boxRect, fontSize * 0.38f, fontSize * 0.38f, bgPaint)
                }
                AnimationTextStyle.GOLD_BANNER -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            boxRect.left, boxRect.top, boxRect.right, boxRect.bottom,
                            Color.argb((alphaInt * 0.88f).toInt(), 98, 68, 16),
                            Color.argb((alphaInt * 0.88f).toInt(), 38, 25, 8),
                            Shader.TileMode.CLAMP,
                        )
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = max(2f, fontSize * 0.06f)
                        color = Color.argb(alphaInt, 255, 214, 112)
                    }
                    canvas.drawRoundRect(boxRect, fontSize * 0.34f, fontSize * 0.34f, bgPaint)
                    canvas.drawRoundRect(boxRect, fontSize * 0.34f, fontSize * 0.34f, borderPaint)
                }
                AnimationTextStyle.NEON -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((alphaInt * 0.35f).toInt(), 20, 14, 52)
                    }
                    canvas.drawRoundRect(boxRect, fontSize * 0.34f, fontSize * 0.34f, bgPaint)
                }
                else -> Unit
            }

            val strokePaint = Paint(fillPaint).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            lines.forEachIndexed { lineIdx, line ->
                val baseline = top + lineHeight * (lineIdx + 0.78f)
                when (overlay.style) {
                    AnimationTextStyle.OUTLINE -> {
                        strokePaint.strokeWidth = max(3f, fontSize * 0.14f)
                        strokePaint.color = Color.argb(alphaInt, 0, 0, 0)
                        strokePaint.setShadowLayer(fontSize * 0.18f, 0f, fontSize * 0.05f, Color.argb((alphaInt * 0.85f).toInt(), 0, 0, 0))
                        canvas.drawText(line, w * 0.5f, baseline, strokePaint)
                    }
                    AnimationTextStyle.NEON -> {
                        strokePaint.strokeWidth = max(2.5f, fontSize * 0.08f)
                        strokePaint.color = Color.argb(alphaInt, 125, 108, 255)
                        strokePaint.setShadowLayer(fontSize * 0.36f, 0f, 0f, Color.argb(alphaInt, 155, 140, 255))
                        canvas.drawText(line, w * 0.5f, baseline, strokePaint)
                        fillPaint.setShadowLayer(fontSize * 0.25f, 0f, 0f, Color.argb(alphaInt, 110, 240, 255))
                    }
                    AnimationTextStyle.CLEAN_SHADOW, AnimationTextStyle.CAPSULE, AnimationTextStyle.GOLD_BANNER -> {
                        fillPaint.setShadowLayer(fontSize * 0.18f, 0f, fontSize * 0.06f, Color.argb((alphaInt * 0.9f).toInt(), 0, 0, 0))
                    }
                }
                fillPaint.color = Color.argb(alphaInt, Color.red(textCol), Color.green(textCol), Color.blue(textCol))
                canvas.drawText(line, w * 0.5f, baseline, fillPaint)
            }
            canvas.restore()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxW: Float): List<String> {
        val result = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            val words = paragraph.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return@forEach
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxW && line.isNotEmpty()) {
                    result.add(line)
                    line = word
                } else {
                    line = candidate
                }
            }
            if (line.isNotEmpty()) result.add(line)
        }
        return if (result.isEmpty()) listOf(text) else result
    }
}
