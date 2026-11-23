package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class SelectedImageData(
    val uri: Uri,
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

object ImageHelper {
    private const val MAX_FILE_BYTES = 10L * 1024L * 1024L
    private val allowedMimes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    suspend fun readAndValidate(context: Context, uri: Uri): Result<SelectedImageData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cr = context.contentResolver
                val rawMime = cr.getType(uri)?.lowercase() ?: "image/jpeg"
                val mime = if (rawMime == "image/jpg") "image/jpeg" else rawMime
                require(mime in allowedMimes) {
                    "Pilih foto JPG, PNG, atau WEBP maksimal 10 MB."
                }

                var fileName = "kangbas-${System.currentTimeMillis()}.jpg"
                var reportedSize = -1L
                cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx >= 0) {
                                cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { fileName = it }
                            }
                            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                                reportedSize = cursor.getLong(sizeIdx)
                            }
                        }
                    }

                if (reportedSize > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("Pilih foto JPG, PNG, atau WEBP maksimal 10 MB.")
                }

                val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Gagal membaca foto yang dipilih.")

                if (bytes.size.toLong() > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("Pilih foto JPG, PNG, atau WEBP maksimal 10 MB.")
                }

                SelectedImageData(
                    uri = uri,
                    bytes = bytes,
                    fileName = fileName,
                    mimeType = mime,
                    sizeBytes = bytes.size.toLong(),
                )
            }
        }

    suspend fun saveCompressedThumbnail(context: Context, imageBytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
                val maxDim = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                var sampleSize = 1
                while (maxDim / sampleSize > 960) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOpts)
                    ?: return@runCatching null

                val scale = minOf(1f, 480f / max(decoded.width, decoded.height).toFloat())
                val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                        if (it !== decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }

                val thumbsDir = File(context.filesDir, "thumbs").apply { mkdirs() }
                val outFile = File(thumbsDir, "thumb-${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 72, fos)
                }
                scaled.recycle()
                outFile.absolutePath
            }.getOrNull()
        }
}
