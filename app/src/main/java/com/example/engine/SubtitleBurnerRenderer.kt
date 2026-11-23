package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSegment
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object SubtitleBurnerRenderer {

    suspend fun burnSubtitlesToMp4(
        context: Context,
        videoUri: Uri,
        segments: List<SubtitleSegment>,
        preset: SubtitleStylePreset,
        position: SubtitlePosition,
        sizeOption: SubtitleSizeOption,
        colorOverride: SubtitleColorOverride,
        onProgress: (Float) -> Unit,
    ): File = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outFile = File(exportsDir, "caption-${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val sortedSegments = segments.sortedBy { it.startMs }
        val typeface = SubtitleCanvasDrawer.loadTypeface(context, preset)

        val overlay = object : BitmapOverlay() {
            private var videoW = 1280
            private var videoH = 720
            private var frameBitmap: Bitmap? = null
            private var lastRenderedKey: String? = "__uninitialized__"

            override fun configure(videoSize: Size) {
                super.configure(videoSize)
                videoW = videoSize.width.coerceAtLeast(2)
                videoH = videoSize.height.coerceAtLeast(2)
                frameBitmap?.recycle()
                frameBitmap = Bitmap.createBitmap(videoW, videoH, Bitmap.Config.ARGB_8888)
                lastRenderedKey = "__uninitialized__"
            }

            override fun getBitmap(presentationTimeUs: Long): Bitmap {
                val bmp = frameBitmap ?: Bitmap.createBitmap(videoW, videoH, Bitmap.Config.ARGB_8888).also {
                    frameBitmap = it
                }
                val timeMs = presentationTimeUs / 1000L
                val active = sortedSegments.firstOrNull { timeMs in it.startMs..it.endMs }
                val nextKey = active?.let { "${it.id}:${it.text}" } ?: "__empty__"

                if (nextKey != lastRenderedKey) {
                    lastRenderedKey = nextKey
                    bmp.eraseColor(Color.TRANSPARENT)
                    if (active != null && active.text.isNotBlank()) {
                        val canvas = Canvas(bmp)
                        SubtitleCanvasDrawer.drawSubtitle(
                            canvas = canvas,
                            width = videoW.toFloat(),
                            height = videoH.toFloat(),
                            rawText = active.text,
                            preset = preset,
                            position = position,
                            sizeOption = sizeOption,
                            colorOverride = colorOverride,
                            typeface = typeface,
                        )
                    }
                }
                return bmp
            }

            override fun release() {
                super.release()
                frameBitmap?.recycle()
                frameBitmap = null
            }
        }

        mainHandler.post {
            if (!continuation.isActive) return@post

            val overlayEffect: Effect = OverlayEffect(ImmutableList.of<TextureOverlay>(overlay))
            val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
                .setEffects(
                    androidx.media3.transformer.Effects(
                        emptyList(),
                        listOf(overlayEffect),
                    )
                )
                .build()

            val progressHolder = ProgressHolder()
            var transformer: Transformer? = null
            val progressRunnable = object : Runnable {
                override fun run() {
                    val t = transformer ?: return
                    if (!continuation.isActive) return
                    val state = t.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((progressHolder.progress / 100f).coerceIn(0f, 0.99f))
                    }
                    mainHandler.postDelayed(this, 180L)
                }
            }

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    mainHandler.removeCallbacks(progressRunnable)
                    onProgress(1.0f)
                    if (continuation.isActive) {
                        continuation.resume(outFile)
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    mainHandler.removeCallbacks(progressRunnable)
                    runCatching { outFile.delete() }
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            }

            transformer = Transformer.Builder(context)
                .addListener(listener)
                .build()

            continuation.invokeOnCancellation {
                mainHandler.post {
                    mainHandler.removeCallbacks(progressRunnable)
                    runCatching { transformer?.cancel() }
                    runCatching { outFile.delete() }
                }
            }

            transformer.start(editedItem, outFile.absolutePath)
            mainHandler.postDelayed(progressRunnable, 150L)
        }
    }
}
