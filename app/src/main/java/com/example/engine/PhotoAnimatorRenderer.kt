package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.example.model.AnimationFontOption
import com.example.model.PhotoAnimationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoAnimatorRenderer {

    suspend fun renderToMp4(
        context: Context,
        photoUri: Uri,
        config: PhotoAnimationConfig,
        audioUri: Uri? = null,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.Default) {
        val decodedBitmap = decodeOrientedBitmap(context, photoUri, maxDim = 1440)
            ?: throw IllegalStateException("Gagal membaca foto untuk dirender.")

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outFile = File(exportsDir, "anim-${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val width = config.ratio.width
        val height = config.ratio.height
        val fps = 30
        val clampedDurationSec = config.durationSeconds.coerceIn(3f, 180f)
        val totalFrames = (clampedDurationSec * fps).roundToInt().coerceAtLeast(30)
        val frameDurationUs = 1_000_000L / fps
        val durationUs = totalFrames * frameDurationUs

        val fontTypefaces = AnimationFontOption.entries.associateWith { fontOpt ->
            runCatching { ResourcesCompat.getFont(context, fontOpt.fontRes) }.getOrNull()
                ?: android.graphics.Typeface.DEFAULT_BOLD
        }
        val subtitleTypeface = SubtitleCanvasDrawer.loadTypeface(context, config.subtitlePreset)

        var videoEncoder: MediaCodec? = null
        var inputSurface: android.view.Surface? = null
        var muxer: MediaMuxer? = null
        var audioExtractor: MediaExtractor? = null
        var muxerStarted = false

        try {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Prepare optional AAC audio track if user picked an .m4a/.aac/mp4 audio file
            var audioTrackSourceIdx = -1
            var audioInputFormat: MediaFormat? = null
            if (audioUri != null) {
                val extractor = MediaExtractor()
                runCatching {
                    extractor.setDataSource(context, audioUri, null)
                    for (i in 0 until extractor.trackCount) {
                        val fmt = extractor.getTrackFormat(i)
                        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                        if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                            audioTrackSourceIdx = i
                            audioInputFormat = fmt
                            extractor.selectTrack(i)
                            break
                        }
                    }
                    if (audioTrackSourceIdx >= 0) {
                        audioExtractor = extractor
                    } else {
                        extractor.release()
                    }
                }.onFailure {
                    runCatching { extractor.release() }
                }
            }

            var videoMuxTrack = -1
            var audioMuxTrack = -1
            val bufferInfo = MediaCodec.BufferInfo()

            fun drainVideoEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    videoEncoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIdx = videoEncoder.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) {
                                throw IllegalStateException("Video format changed after muxer started.")
                            }
                            videoMuxTrack = muxer.addTrack(videoEncoder.outputFormat)
                            if (audioInputFormat != null) {
                                audioMuxTrack = muxer.addTrack(audioInputFormat!!)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        outIdx >= 0 -> {
                            val encoded = videoEncoder.getOutputBuffer(outIdx)
                            if (encoded != null && bufferInfo.size > 0 &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                                muxerStarted
                            ) {
                                encoded.position(bufferInfo.offset)
                                encoded.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(videoMuxTrack, encoded, bufferInfo)
                            }
                            videoEncoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val timeSec = frameIdx.toFloat() / fps.toFloat()
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }
                try {
                    EffectCanvasDrawer.drawFrame(
                        canvas = canvas,
                        width = width.toFloat(),
                        height = height.toFloat(),
                        photoBitmap = decodedBitmap,
                        config = config,
                        timeSec = timeSec,
                        fontTypefaces = fontTypefaces,
                        subtitleTypeface = subtitleTypeface,
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                drainVideoEncoder(endOfStream = false)
                if (frameIdx % 4 == 0 || frameIdx == totalFrames - 1) {
                    val videoRatio = if (audioExtractor != null) 0.9f else 1.0f
                    onProgress(((frameIdx + 1).toFloat() / totalFrames.toFloat()) * videoRatio)
                }
            }

            drainVideoEncoder(endOfStream = true)

            // Write AAC audio samples (looping if shorter than target video duration)
            if (audioExtractor != null && muxerStarted && audioMuxTrack >= 0) {
                val maxSampleSize = (audioInputFormat?.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) ?: 128 * 1024)
                    .coerceAtLeast(128 * 1024)
                val audioBuf = ByteBuffer.allocateDirect(maxSampleSize)
                val audioInfo = MediaCodec.BufferInfo()
                var loopOffsetUs = 0L
                var lastPassMaxPtsUs = 0L

                while (loopOffsetUs < durationUs) {
                    coroutineContext.ensureActive()
                    audioExtractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    var wroteAnyInPass = false
                    while (true) {
                        audioBuf.clear()
                        val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                        if (sampleSize < 0) break
                        val sampleTimeUs = audioExtractor.sampleTime
                        val targetPtsUs = loopOffsetUs + sampleTimeUs
                        if (targetPtsUs > durationUs) {
                            break
                        }
                        audioInfo.offset = 0
                        audioInfo.size = sampleSize
                        audioInfo.presentationTimeUs = targetPtsUs
                        audioInfo.flags = audioExtractor.sampleFlags
                        audioBuf.limit(sampleSize)
                        muxer.writeSampleData(audioMuxTrack, audioBuf, audioInfo)
                        lastPassMaxPtsUs = max(lastPassMaxPtsUs, targetPtsUs)
                        wroteAnyInPass = true
                        audioExtractor.advance()
                    }
                    if (!wroteAnyInPass || lastPassMaxPtsUs <= loopOffsetUs) break
                    loopOffsetUs = lastPassMaxPtsUs + 23_220L
                }
                onProgress(1.0f)
            } else {
                onProgress(1.0f)
            }

            outFile
        } catch (t: Throwable) {
            outFile.delete()
            throw t
        } finally {
            runCatching { audioExtractor?.release() }
            runCatching {
                if (muxerStarted) {
                    muxer?.stop()
                }
            }
            runCatching { muxer?.release() }
            runCatching { videoEncoder?.stop() }
            runCatching { videoEncoder?.release() }
            runCatching { inputSurface?.release() }
            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
        }
    }

    private fun decodeOrientedBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val rawMax = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sampleSize = 1
        while (rawMax / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            runCatching {
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }.getOrDefault(0f)
        } ?: 0f

        if (rotationDegrees == 0f) return rawBitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        if (rotated !== rawBitmap) {
            rawBitmap.recycle()
        }
        return rotated
    }
}
