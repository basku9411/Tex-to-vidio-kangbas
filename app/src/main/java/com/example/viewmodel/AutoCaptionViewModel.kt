package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedVideoDraft
import com.example.data.local.VideoDraftStore
import com.example.data.local.VideoHistoryEntity
import com.example.data.local.VideoHistoryRepository
import com.example.data.remote.AutoCaptionAiRepository
import com.example.engine.AudioExtractor
import com.example.engine.SubtitleBurnerRenderer
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSegment
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import com.example.model.VideoStatus
import com.example.util.VideoDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TranscriptionUiState {
    data object Idle : TranscriptionUiState
    data object Loading : TranscriptionUiState
    data class Success(val count: Int) : TranscriptionUiState
    data class Error(val message: String) : TranscriptionUiState
}

data class AutoCaptionUiState(
    val selectedVideoUri: Uri? = null,
    val selectedVideoTitle: String = "",
    val videoDurationMs: Long = 10_000L,
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val transcriptionState: TranscriptionUiState = TranscriptionUiState.Idle,
    val segments: List<SubtitleSegment> = emptyList(),
    val selectedPreset: SubtitleStylePreset = SubtitleStylePreset.TIKTOK_POP,
    val selectedPosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val selectedSize: SubtitleSizeOption = SubtitleSizeOption.MEDIUM,
    val selectedColor: SubtitleColorOverride = SubtitleColorOverride.PRESET,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportedFilePath: String? = null,
    val exportError: String? = null,
    val snackbarMessage: String? = null,
)

class AutoCaptionViewModel(
    application: Application,
    private val userId: String,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val aiRepository = AutoCaptionAiRepository()
    private val historyRepo = VideoHistoryRepository(appContext, userId)

    private val _uiState = MutableStateFlow(AutoCaptionUiState())
    val uiState: StateFlow<AutoCaptionUiState> = _uiState.asStateFlow()

    private var transcribeJob: Job? = null
    private var exportJob: Job? = null

    fun onVideoPicked(uri: Uri?, customTitle: String? = null) {
        if (uri == null) return
        viewModelScope.launch {
            val title = customTitle ?: queryDisplayName(uri) ?: "video-pilihan.mp4"
            val meta = inspectVideoMeta(uri)
            _uiState.update {
                it.copy(
                    selectedVideoUri = uri,
                    selectedVideoTitle = title,
                    videoDurationMs = meta.first,
                    videoWidth = meta.second,
                    videoHeight = meta.third,
                    transcriptionState = TranscriptionUiState.Idle,
                    segments = emptyList(),
                    exportedFilePath = null,
                    exportError = null,
                    snackbarMessage = "Video dipilih: $title",
                )
            }
        }
    }

    fun selectFromHistory(item: VideoHistoryEntity) {
        val rawUrl = item.videoUrl ?: return
        val uri = rawUrl.toUri()
        if (uri.scheme == "http" || uri.scheme == "https") {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Unduh video online ini ke perangkat terlebih dahulu agar bisa diproses.",
                )
            }
            return
        }
        onVideoPicked(uri, customTitle = item.title)
    }

    fun hasSavedDraft(item: VideoHistoryEntity): Boolean =
        VideoDraftStore.hasDraft(appContext, item.id)

    fun restoreFromHistoryForEdit(item: VideoHistoryEntity) {
        viewModelScope.launch {
            val draft = withContext(Dispatchers.IO) {
                VideoDraftStore.loadDraft(appContext, item.id) as? SavedVideoDraft.AutoCaption
            }
            if (draft != null) {
                val uri = draft.videoUriString.toUri()
                val meta = inspectVideoMeta(uri)
                _uiState.update {
                    it.copy(
                        selectedVideoUri = uri,
                        selectedVideoTitle = draft.videoTitle,
                        videoDurationMs = meta.first,
                        videoWidth = meta.second,
                        videoHeight = meta.third,
                        transcriptionState = if (draft.segments.isNotEmpty()) {
                            TranscriptionUiState.Success(draft.segments.size)
                        } else {
                            TranscriptionUiState.Idle
                        },
                        segments = draft.segments,
                        selectedPreset = draft.preset,
                        selectedPosition = draft.position,
                        selectedSize = draft.size,
                        selectedColor = draft.color,
                        exportedFilePath = null,
                        exportError = null,
                        snackbarMessage = "Proyek subtitle dimuat ulang untuk diedit lagi!",
                    )
                }
            } else {
                selectFromHistory(item)
            }
        }
    }

    fun generateAutoCaptions() {
        val videoUri = _uiState.value.selectedVideoUri
        if (videoUri == null) {
            _uiState.update { it.copy(snackbarMessage = "Pilih video terlebih dahulu.") }
            return
        }
        transcribeJob?.cancel()
        transcribeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    transcriptionState = TranscriptionUiState.Loading,
                    exportError = null,
                )
            }
            try {
                val extracted = AudioExtractor.extractForTranscription(appContext, videoUri)
                val resultSegments = aiRepository.transcribeAudio(
                    mediaBytes = extracted.bytes,
                    mimeType = extracted.mimeType,
                    videoDurationMs = extracted.videoDurationMs,
                )
                _uiState.update {
                    it.copy(
                        videoDurationMs = extracted.videoDurationMs,
                        videoWidth = extracted.videoWidth,
                        videoHeight = extracted.videoHeight,
                        segments = resultSegments,
                        transcriptionState = TranscriptionUiState.Success(resultSegments.size),
                        snackbarMessage = if (resultSegments.isEmpty()) {
                            "Tidak ada suara ucapan terdeteksi; Anda bisa tambah teks manual."
                        } else {
                            "${resultSegments.size} baris teks otomatis berhasil dibuat!"
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        transcriptionState = TranscriptionUiState.Error(
                            e.localizedMessage ?: "Gagal memproses teks otomatis dari audio."
                        )
                    )
                }
            }
        }
    }

    fun selectPreset(preset: SubtitleStylePreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun selectPosition(position: SubtitlePosition) {
        _uiState.update { it.copy(selectedPosition = position) }
    }

    fun selectSize(size: SubtitleSizeOption) {
        _uiState.update { it.copy(selectedSize = size) }
    }

    fun selectColor(color: SubtitleColorOverride) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    fun updateSegmentText(id: String, text: String) {
        _uiState.update { state ->
            state.copy(
                segments = state.segments.map { seg ->
                    if (seg.id == id) seg.copy(text = text) else seg
                }
            )
        }
    }

    fun shiftSegmentTime(id: String, deltaStartMs: Long, deltaEndMs: Long) {
        val maxDur = _uiState.value.videoDurationMs.coerceAtLeast(1000L)
        _uiState.update { state ->
            state.copy(
                segments = state.segments.map { seg ->
                    if (seg.id == id) {
                        val nextStart = (seg.startMs + deltaStartMs).coerceIn(0L, maxDur - 250L)
                        val nextEnd = (seg.endMs + deltaEndMs).coerceIn(nextStart + 250L, maxDur)
                        seg.copy(startMs = nextStart, endMs = nextEnd)
                    } else {
                        seg
                    }
                }
            )
        }
    }

    fun deleteSegment(id: String) {
        _uiState.update { state ->
            state.copy(segments = state.segments.filterNot { it.id == id })
        }
    }

    fun addManualSegment() {
        val state = _uiState.value
        val maxDur = state.videoDurationMs.coerceAtLeast(2000L)
        val lastEnd = state.segments.maxOfOrNull { it.endMs } ?: 0L
        val start = lastEnd.coerceAtMost((maxDur - 1500L).coerceAtLeast(0L))
        val end = (start + 2000L).coerceAtMost(maxDur)
        val newSeg = SubtitleSegment(
            id = "manual-${System.currentTimeMillis()}",
            startMs = start,
            endMs = end.coerceAtLeast(start + 300L),
            text = "Teks baru di sini",
        )
        _uiState.update {
            it.copy(segments = (it.segments + newSeg).sortedBy { s -> s.startMs })
        }
    }

    fun exportVideoWithSubtitles() {
        val state = _uiState.value
        val videoUri = state.selectedVideoUri
        if (videoUri == null) {
            _uiState.update { it.copy(snackbarMessage = "Pilih video terlebih dahulu.") }
            return
        }
        val activeSegments = state.segments.filter { it.text.isNotBlank() }
        if (activeSegments.isEmpty()) {
            _uiState.update {
                it.copy(snackbarMessage = "Buat teks otomatis atau tambah minimal 1 baris teks dulu.")
            }
            return
        }
        if (state.isExporting) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportProgress = 0.02f,
                    exportedFilePath = null,
                    exportError = null,
                )
            }
            try {
                val outFile = SubtitleBurnerRenderer.burnSubtitlesToMp4(
                    context = appContext,
                    videoUri = videoUri,
                    segments = activeSegments,
                    preset = state.selectedPreset,
                    position = state.selectedPosition,
                    sizeOption = state.selectedSize,
                    colorOverride = state.selectedColor,
                    onProgress = { progress ->
                        _uiState.update { it.copy(exportProgress = progress) }
                    },
                )

                val fileUri = Uri.fromFile(outFile).toString()
                val title = "Teks: ${state.selectedVideoTitle.substringBeforeLast('.')}"
                val historyId = "caption-${System.currentTimeMillis()}"
                withContext(Dispatchers.IO) {
                    VideoDraftStore.saveAutoCaptionDraft(
                        context = appContext,
                        historyId = historyId,
                        videoUri = videoUri,
                        videoTitle = state.selectedVideoTitle,
                        preset = state.selectedPreset,
                        position = state.selectedPosition,
                        size = state.selectedSize,
                        color = state.selectedColor,
                        segments = activeSegments,
                    )
                }
                historyRepo.upsert(
                    VideoHistoryEntity(
                        id = historyId,
                        userId = userId,
                        title = title.take(72),
                        modelLabel = "Teks Otomatis · ${state.selectedPreset.label}",
                        mode = "caption",
                        status = VideoStatus.COMPLETED.wireValue,
                        progressText = "SELESAI",
                        thumbPath = null,
                        taskId = null,
                        videoUrl = fileUri,
                        error = null,
                        createdAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis(),
                        estimatedCredits = 0,
                    )
                )

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 1f,
                        exportedFilePath = outFile.absolutePath,
                        snackbarMessage = "Video berteks selesai & masuk Galeri! Tekan Unduh Video untuk menyimpan.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = e.localizedMessage ?: "Gagal menempelkan teks ke video.",
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                isExporting = false,
                exportProgress = 0f,
                snackbarMessage = "Ekspor video dibatalkan.",
            )
        }
    }

    fun clearExportResult() {
        _uiState.update {
            it.copy(
                exportedFilePath = null,
                exportProgress = 0f,
                exportError = null,
            )
        }
    }

    fun downloadExportedVideo() {
        val path = _uiState.value.exportedFilePath ?: return
        val baseTitle = _uiState.value.selectedVideoTitle.substringBeforeLast('.').ifBlank { "teks-otomatis" }
        val res = VideoDownloader.saveOrDownloadVideo(
            context = appContext,
            videoUrl = "file://$path",
            title = "teks-$baseTitle",
        )
        _uiState.update {
            it.copy(
                snackbarMessage = res.getOrElse { err ->
                    "Gagal mengunduh video: ${err.message ?: "kesalahan"}"
                }
            )
        }
    }

    fun openExportedExternal() {
        val path = _uiState.value.exportedFilePath ?: return
        if (!VideoDownloader.openExternal(appContext, "file://$path")) {
            _uiState.update {
                it.copy(snackbarMessage = "Tidak ada aplikasi pemutar video yang ditemukan.")
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun queryDisplayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private suspend fun inspectVideoMeta(uri: Uri): Triple<Long, Int, Int> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10_000L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            Triple(dur.coerceAtLeast(1000L), w.coerceAtLeast(2), h.coerceAtLeast(2))
        } catch (_: Exception) {
            Triple(10_000L, 1280, 720)
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        fun factory(application: Application, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AutoCaptionViewModel(application, userId) as T
                }
            }
    }
}
