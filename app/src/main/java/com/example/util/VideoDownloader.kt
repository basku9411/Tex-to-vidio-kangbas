package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

object VideoDownloader {
    fun saveOrDownloadVideo(context: Context, videoUrl: String, title: String): Result<String> {
        return runCatching {
            val safeName = title
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(28)
                .ifBlank { "video" }
            val fileName = "kangbas-$safeName-${System.currentTimeMillis() % 100000}.mp4"

            val uri = videoUrl.toUri()
            if (uri.scheme == "file" || videoUrl.startsWith("/")) {
                val sourceFile = File(uri.path ?: videoUrl)
                require(sourceFile.exists()) { "File video tidak ditemukan." }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kangbas")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Gagal membuat file di Downloads.")
                    resolver.openOutputStream(destUri)?.use { out ->
                        sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(destUri, values, null, null)
                    "Video disimpan ke Downloads/Kangbas/$fileName"
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetDir = File(downloadsDir, "Kangbas").apply { mkdirs() }
                    val targetFile = File(targetDir, fileName)
                    sourceFile.copyTo(targetFile, overwrite = true)
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("video/mp4"), null)
                    "Video disimpan ke ${targetFile.absolutePath}"
                }
            } else {
                val request = DownloadManager.Request(uri)
                    .setTitle("Kangbas: $title")
                    .setDescription("Mengunduh video AI...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                "Mengunduh video ke folder Downloads…"
            }
        }
    }

    fun openExternal(context: Context, videoUrl: String): Boolean {
        val rawUri = videoUrl.toUri()
        val playUri: Uri = if (rawUri.scheme == "file" || videoUrl.startsWith("/")) {
            val file = File(rawUri.path ?: videoUrl)
            if (!file.exists()) return false
            runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrElse { return false }
        } else {
            rawUri
        }

        val videoIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(playUri, "video/mp4")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, playUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(videoIntent)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(browserIntent)
                true
            }.getOrDefault(false)
        }
    }

    fun openWebUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
