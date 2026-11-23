package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object SubtitleCanvasDrawer {

    fun loadTypeface(context: Context, preset: SubtitleStylePreset): Typeface {
        return runCatching {
            ResourcesCompat.getFont(context, preset.fontRes)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }

    fun drawSubtitle(
        canvas: Canvas,
        width: Float,
        height: Float,
        rawText: String,
        preset: SubtitleStylePreset,
        position: SubtitlePosition,
        sizeOption: SubtitleSizeOption,
        colorOverride: SubtitleColorOverride,
        typeface: Typeface,
    ) {
        val cleanText = rawText.trim()
        if (cleanText.isEmpty() || width <= 0f || height <= 0f) return

        val displayText = if (preset.uppercase) {
            cleanText.uppercase(Locale.forLanguageTag("id-ID"))
        } else {
            cleanText
        }

        val minDim = minOf(width, height)
        val fontSizePx = (minDim * sizeOption.widthFractionFontSize).coerceIn(16f, 140f)
        val maxContentWidth = (width * 0.86f).roundToInt().coerceAtLeast(80)

        val fillColorInt = (colorOverride.colorHex ?: preset.textColorHex).toInt()
        val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = fontSizePx
            color = fillColorInt
        }

        val layout = buildStaticLayout(displayText, fillPaint, maxContentWidth)
        var maxLineWidth = 0f
        for (i in 0 until layout.lineCount) {
            maxLineWidth = max(maxLineWidth, layout.getLineWidth(i))
        }
        val blockWidth = maxLineWidth.coerceAtLeast(24f)
        val blockHeight = layout.height.toFloat().coerceAtLeast(fontSizePx)

        val padX = fontSizePx * 0.55f
        val padY = fontSizePx * 0.28f
        val centerY = height * position.verticalCenterFraction
        val clampedTop = (centerY - blockHeight * 0.5f)
            .coerceIn(height * 0.06f + padY, (height * 0.94f - blockHeight - padY).coerceAtLeast(height * 0.06f))
        val blockLeft = (width - maxContentWidth) * 0.5f
        val boxLeft = (width - blockWidth) * 0.5f - padX
        val boxRight = (width + blockWidth) * 0.5f + padX
        val boxTop = clampedTop - padY
        val boxBottom = clampedTop + blockHeight + padY

        // 1. Optional rounded capsule/box background
        preset.boxColorHex?.let { bgHex ->
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgHex.toInt()
                style = Paint.Style.FILL
            }
            val radius = fontSizePx * 0.38f
            canvas.drawRoundRect(RectF(boxLeft, boxTop, boxRight, boxBottom), radius, radius, bgPaint)
        }

        canvas.save()
        canvas.translate(blockLeft, clampedTop)

        // 2. Optional thick outline stroke
        if (preset.strokeWidthRatio > 0f && (preset.strokeColorHex ushr 24) > 0L) {
            val strokePaint = TextPaint(fillPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = (fontSizePx * preset.strokeWidthRatio).coerceAtLeast(2f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = preset.strokeColorHex.toInt()
                if (preset.glowRadius > 0f) {
                    setShadowLayer(preset.glowRadius, 0f, 0f, preset.shadowColorHex.toInt())
                } else {
                    setShadowLayer(fontSizePx * 0.14f, 0f, fontSizePx * 0.06f, preset.shadowColorHex.toInt())
                }
            }
            buildStaticLayout(displayText, strokePaint, maxContentWidth).draw(canvas)
        } else {
            val shadowR = if (preset.glowRadius > 0f) preset.glowRadius else fontSizePx * 0.14f
            fillPaint.setShadowLayer(shadowR, 0f, if (preset.glowRadius > 0f) 0f else fontSizePx * 0.06f, preset.shadowColorHex.toInt())
        }

        // 3. Main crisp fill pass
        layout.draw(canvas)
        canvas.restore()
    }

    @Suppress("DEPRECATION")
    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(false)
            .build()
    }
}
