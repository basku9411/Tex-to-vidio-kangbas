package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class ExtractedAudioPayload(
    val bytes: ByteArray,
    val mimeType: String,
    val videoDurationMs: Long,
    val videoWidth: Int,
    val videoHeight: Int,
)

object AudioExtractor {
    private const val MAX_AI_INLINE_BYTES = 19 * 1024 * 1024 // < 20 MB Firebase AI limit

    suspend fun extractForTranscription(
        context: Context,
        videoUri: Uri,
    ): ExtractedAudioPayload = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 1280
        var height = 720
        var hasAudio = false

        try {
            retriever.setDataSource(context, videoUri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1280
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 720
            hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
        } finally {
            runCatching { retriever.release() }
        }

        if (!hasAudio) {
            val mimeHint = context.contentResolver.getType(videoUri).orEmpty()
            if (!mimeHint.startsWith("audio/")) {
                throw IllegalStateException("Media ini tidak memiliki trek audio/suara untuk dibuat teks otomatis.")
            }
        }

        val extractor = MediaExtractor()
        val tempAudioFile = File(context.cacheDir, "audio_extract_${System.currentTimeMillis()}.m4a")
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            extractor.setDataSource(context, videoUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                throw IllegalStateException("Trek audio tidak ditemukan dalam video ini.")
            }

            val audioMime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            // Fast path: if audio is already AAC/MP4A, remux directly into .m4a container
            if (audioMime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                extractor.selectTrack(audioTrackIndex)
                muxer = MediaMuxer(tempAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxTrack = muxer.addTrack(audioFormat)
                muxer.start()
                muxerStarted = true

                val maxBuf = (audioFormat.getIntegerOrDefault(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024))
                    .coerceAtLeast(256 * 1024)
                val buffer = ByteBuffer.allocate(maxBuf)
                val info = MediaCodec.BufferInfo()
                while (true) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = extractor.sampleFlags
                    buffer.limit(size)
                    muxer.writeSampleData(muxTrack, buffer, info)
                    extractor.advance()
                }
                muxer.stop()
                muxerStarted = false
                muxer.release()
                muxer = null

                val bytes = tempAudioFile.readBytes()
                if (bytes.isEmpty()) {
                    throw IllegalStateException("Audio hasil ekstraksi kosong.")
                }
                if (bytes.size > MAX_AI_INLINE_BYTES) {
                    throw IllegalStateException("Ukuran audio terlalu besar (>19 MB). Gunakan video yang lebih pendek.")
                }
                return@withContext ExtractedAudioPayload(
                    bytes = bytes,
                    mimeType = "audio/mp4",
                    videoDurationMs = durationMs.coerceAtLeast(1000L),
                    videoWidth = width,
                    videoHeight = height,
                )
            } else {
                // Fallback: if non-AAC video file itself is compact (< 19 MB), pass video/mp4 inline directly
                val rawBytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Gagal membaca file video.")
                if (rawBytes.size > MAX_AI_INLINE_BYTES) {
                    throw IllegalStateException("Ukuran video dengan format audio khusus melebihi batas 19 MB. Gunakan video MP4 (AAC) atau durasi lebih singkat.")
                }
                val detectedMime = context.contentResolver.getType(videoUri)
                    ?: if (audioMime.startsWith("audio/")) audioMime else "video/mp4"
                return@withContext ExtractedAudioPayload(
                    bytes = rawBytes,
                    mimeType = detectedMime,
                    videoDurationMs = durationMs.coerceAtLeast(1000L),
                    videoWidth = width,
                    videoHeight = height,
                )
            }
        } finally {
            runCatching {
                if (muxerStarted) muxer?.stop()
            }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            runCatching { tempAudioFile.delete() }
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int {
        return if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(default) else default
    }
}
