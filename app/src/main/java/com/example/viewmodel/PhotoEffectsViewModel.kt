package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
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
import com.example.engine.PhotoAnimatorRenderer
import com.example.model.AnimationFontOption
import com.example.model.AnimationStudioMode
import com.example.model.AnimationTextColor
import com.example.model.AnimationTextMotion
import com.example.model.AnimationTextStyle
import com.example.model.EffectColorTheme
import com.example.model.EffectDensity
import com.example.model.EffectSpeed
import com.example.model.PhotoAnimationConfig
import com.example.model.PhotoEffect
import com.example.model.PhotoTextOverlay
import com.example.model.PhotoVideoRatio
import com.example.model.SubtitleSegment
import com.example.model.SubtitleStylePreset
import com.example.model.VideoStatus
import com.example.util.ImageHelper
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class TextOverlayDraft(
    val editingId: String? = null,
    val text: String = "",
    val font: AnimationFontOption = AnimationFontOption.PLAYFAIR,
    val color: AnimationTextColor = AnimationTextColor.WHITE,
    val style: AnimationTextStyle = AnimationTextStyle.OUTLINE,
    val motion: AnimationTextMotion = AnimationTextMotion.FLOAT,
    val sizeFraction: Float = 0.068f,
    val yFraction: Float = 0.80f,
)

data class PhotoEffectsUiState(
    val studioMode: AnimationStudioMode = AnimationStudioMode.PHOTO_ANIMATION,
    val selectedPhotoUri: Uri? = null,
    val selectedPhotoName: String = "",
    val previewBitmap: Bitmap? = null,
    val selectedAudioUri: Uri? = null,
    val selectedAudioName: String? = null,
    val detectedAudioDurationSec: Float? = null,
    val referenceVideoName: String? = null,
    val referenceVideoDurationSec: Float? = null,
    val config: PhotoAnimationConfig = PhotoAnimationConfig(),
    val textDraft: TextOverlayDraft = TextOverlayDraft(),
    val isTranscribingAudio: Boolean = false,
    val audioTranscribeError: String? = null,
    val isRendering: Boolean = false,
    val renderProgress: Float = 0f,
    val exportedFilePath: String? = null,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    val snackbarMessage: String? = null,
)

class PhotoEffectsViewModel(
    application: Application,
    private val userId: String,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val historyRepo = VideoHistoryRepository(appContext, userId)
    private val aiRepository = AutoCaptionAiRepository()

    private val _uiState = MutableStateFlow(PhotoEffectsUiState())
    val uiState: StateFlow<PhotoEffectsUiState> = _uiState.asStateFlow()

    private var renderJob: Job? = null
    private var transcribeJob: Job? = null

    fun setStudioMode(mode: AnimationStudioMode) {
        _uiState.update { it.copy(studioMode = mode) }
    }

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val fileName = queryDisplayName(uri) ?: "foto-anda.jpg"
            val bmp = decodePreviewBitmap(uri)
            if (bmp == null) {
                _uiState.update {
                    it.copy(snackbarMessage = "Gagal membuka foto. Pilih file JPG, PNG, atau WEBP.")
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    selectedPhotoUri = uri,
                    selectedPhotoName = fileName,
                    previewBitmap = bmp,
                    exportedFilePath = null,
                    statusMessage = "Foto dimuat: wajah & teks asli tetap terjaga 100%.",
                    statusIsError = false,
                )
            }
        }
    }

    fun onAudioPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val name = queryDisplayName(uri) ?: "audio-latar.m4a"
            val durSec = extractMediaDurationSeconds(uri)
            _uiState.update { state ->
                val updatedConfig = if (durSec != null) {
                    state.config.copy(
                        durationSeconds = durSec,
                        durationSourceLabel = "Sesuai Audio ($name)",
                    )
                } else {
                    state.config
                }
                state.copy(
                    selectedAudioUri = uri,
                    selectedAudioName = name,
                    detectedAudioDurationSec = durSec,
                    config = updatedConfig,
                    snackbarMessage = if (durSec != null) {
                        "Audio dipilih & durasi disamakan: ${formatSec(durSec)}"
                    } else {
                        "Audio dipilih: $name"
                    },
                )
            }
        }
    }

    fun onReferenceVideoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val name = queryDisplayName(uri) ?: "video-acuan.mp4"
            val durSec = extractMediaDurationSeconds(uri)
            if (durSec == null) {
                _uiState.update {
                    it.copy(snackbarMessage = "Tidak bisa membaca durasi video tersebut.")
                }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    referenceVideoName = name,
                    referenceVideoDurationSec = durSec,
                    config = state.config.copy(
                        durationSeconds = durSec,
                        durationSourceLabel = "Sesuai Video ($name)",
                    ),
                    snackbarMessage = "Durasi animasi disamakan dengan video: ${formatSec(durSec)}",
                )
            }
        }
    }

    fun applyReferenceVideoDuration() {
        val state = _uiState.value
        val durSec = state.referenceVideoDurationSec ?: return
        val name = state.referenceVideoName ?: "video"
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    durationSeconds = durSec,
                    durationSourceLabel = "Sesuai Video ($name)",
                )
            )
        }
    }

    fun applyAudioDuration() {
        val state = _uiState.value
        val durSec = state.detectedAudioDurationSec ?: return
        val name = state.selectedAudioName ?: "audio"
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    durationSeconds = durSec,
                    durationSourceLabel = "Sesuai Audio ($name)",
                )
            )
        }
    }

    fun clearAudio() {
        _uiState.update {
            it.copy(
                selectedAudioUri = null,
                selectedAudioName = null,
                detectedAudioDurationSec = null,
            )
        }
    }

    fun toggleEffect(effect: PhotoEffect) {
        _uiState.update { state ->
            val current = state.config.selectedEffects
            val next = if (effect in current) current - effect else current + effect
            state.copy(config = state.config.copy(selectedEffects = next))
        }
    }

    fun applyQuickPreset(presetName: String) {
        val effects = when (presetName) {
            "doremi_birds" -> setOf(
                PhotoEffect.MUSIC_NOTES,
                PhotoEffect.FLYING_BIRDS,
                PhotoEffect.FIREFLIES,
                PhotoEffect.KEN_BURNS,
            )
            "dove_peace" -> setOf(
                PhotoEffect.DOVES_WHITE,
                PhotoEffect.FLOATING_FEATHERS,
                PhotoEffect.CRESCENT_STARS,
                PhotoEffect.GOLD_DUST,
            )
            "party_music" -> setOf(
                PhotoEffect.MUSIC_NOTES,
                PhotoEffect.EQUALIZER_BARS,
                PhotoEffect.CONFETTI,
                PhotoEffect.FAIRY_ORBS,
            )
            "magic_night" -> setOf(
                PhotoEffect.FIREFLIES,
                PhotoEffect.SPARKLES,
                PhotoEffect.SHOOTING_STARS,
                PhotoEffect.LANTERNS,
                PhotoEffect.GLOW_FRAME,
            )
            "sakura_dream" -> setOf(
                PhotoEffect.SAKURA,
                PhotoEffect.BUTTERFLIES,
                PhotoEffect.DANDELION_SEEDS,
                PhotoEffect.LIGHT_BEAMS,
            )
            "rain_cozy" -> setOf(
                PhotoEffect.RAIN,
                PhotoEffect.WATER_RIPPLES,
                PhotoEffect.BOKEH,
                PhotoEffect.GLOW_FRAME,
            )
            else -> setOf(PhotoEffect.MUSIC_NOTES, PhotoEffect.FIREFLIES)
        }
        _uiState.update { it.copy(config = it.config.copy(selectedEffects = effects)) }
    }

    fun clearAllEffects() {
        _uiState.update { it.copy(config = it.config.copy(selectedEffects = emptySet())) }
    }

    fun setDensity(density: EffectDensity) {
        _uiState.update { it.copy(config = it.config.copy(density = density)) }
    }

    fun setSpeed(speed: EffectSpeed) {
        _uiState.update { it.copy(config = it.config.copy(speed = speed)) }
    }

    fun setColorTheme(theme: EffectColorTheme) {
        _uiState.update { it.copy(config = it.config.copy(colorTheme = theme)) }
    }

    fun setRatio(ratio: PhotoVideoRatio) {
        _uiState.update { it.copy(config = it.config.copy(ratio = ratio)) }
    }

    fun setDurationSeconds(seconds: Float, sourceLabel: String? = null) {
        val clamped = seconds.coerceIn(3f, 180f)
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    durationSeconds = clamped,
                    durationSourceLabel = sourceLabel,
                )
            )
        }
    }

    // --- Manual Text Overlay Controls ---
    fun updateDraftText(text: String) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(text = text)) }
    }

    fun selectDraftFont(font: AnimationFontOption) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(font = font)) }
    }

    fun selectDraftColor(color: AnimationTextColor) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(color = color)) }
    }

    fun selectDraftStyle(style: AnimationTextStyle) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(style = style)) }
    }

    fun selectDraftMotion(motion: AnimationTextMotion) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(motion = motion)) }
    }

    fun setDraftSizeFraction(size: Float) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(sizeFraction = size.coerceIn(0.035f, 0.13f))) }
    }

    fun setDraftYFraction(y: Float) {
        _uiState.update { it.copy(textDraft = it.textDraft.copy(yFraction = y.coerceIn(0.10f, 0.90f))) }
    }

    fun saveDraftTextOverlay() {
        val draft = _uiState.value.textDraft
        val clean = draft.text.trim()
        if (clean.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Tulis teks terlebih dahulu sebelum ditambahkan.") }
            return
        }
        val item = PhotoTextOverlay(
            id = draft.editingId ?: "txt-${System.currentTimeMillis()}",
            text = clean,
            font = draft.font,
            sizeFraction = draft.sizeFraction,
            yFraction = draft.yFraction,
            color = draft.color,
            style = draft.style,
            motion = draft.motion,
        )
        _uiState.update { state ->
            val current = state.config.textOverlays
            val next = if (draft.editingId != null) {
                current.map { if (it.id == draft.editingId) item else it }
            } else {
                current + item
            }
            state.copy(
                config = state.config.copy(textOverlays = next),
                textDraft = draft.copy(editingId = null, text = ""),
                snackbarMessage = if (draft.editingId != null) "Teks animasi diperbarui." else "Teks ditambahkan ke pratinjau animasi!",
            )
        }
    }

    fun editTextOverlay(overlay: PhotoTextOverlay) {
        _uiState.update {
            it.copy(
                textDraft = TextOverlayDraft(
                    editingId = overlay.id,
                    text = overlay.text,
                    font = overlay.font,
                    color = overlay.color,
                    style = overlay.style,
                    motion = overlay.motion,
                    sizeFraction = overlay.sizeFraction,
                    yFraction = overlay.yFraction,
                )
            )
        }
    }

    fun cancelEditingText() {
        _uiState.update {
            it.copy(textDraft = it.textDraft.copy(editingId = null, text = ""))
        }
    }

    fun removeTextOverlay(id: String) {
        _uiState.update { state ->
            val next = state.config.textOverlays.filterNot { it.id == id }
            val nextDraft = if (state.textDraft.editingId == id) {
                state.textDraft.copy(editingId = null, text = "")
            } else {
                state.textDraft
            }
            state.copy(
                config = state.config.copy(textOverlays = next),
                textDraft = nextDraft,
            )
        }
    }

    // --- Timed AI Auto-Text from Audio ---
    fun generateTimedSubtitlesFromAudio() {
        val audioUri = _uiState.value.selectedAudioUri
        if (audioUri == null) {
            _uiState.update {
                it.copy(snackbarMessage = "Pilih musik/audio dulu di tab Audio untuk membuat teks otomatis.")
            }
            return
        }
        transcribeJob?.cancel()
        transcribeJob = viewModelScope.launch {
            _uiState.update { it.copy(isTranscribingAudio = true, audioTranscribeError = null) }
            try {
                val extracted = AudioExtractor.extractForTranscription(appContext, audioUri)
                val segs = aiRepository.transcribeAudio(
                    mediaBytes = extracted.bytes,
                    mimeType = extracted.mimeType,
                    videoDurationMs = extracted.videoDurationMs,
                )
                _uiState.update {
                    it.copy(
                        isTranscribingAudio = false,
                        config = it.config.copy(timedSubtitles = segs),
                        snackbarMessage = if (segs.isEmpty()) {
                            "Tidak ada suara ucapan/vokal terdeteksi di audio ini."
                        } else {
                            "${segs.size} teks otomatis dari audio ditambahkan ke animasi!"
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTranscribingAudio = false,
                        audioTranscribeError = e.localizedMessage ?: "Gagal membuat teks otomatis dari audio.",
                    )
                }
            }
        }
    }

    fun selectTimedSubtitlePreset(preset: SubtitleStylePreset) {
        _uiState.update { it.copy(config = it.config.copy(subtitlePreset = preset)) }
    }

    fun updateTimedSubtitleText(id: String, text: String) {
        _uiState.update { state ->
            state.copy(
                config = state.config.copy(
                    timedSubtitles = state.config.timedSubtitles.map { seg ->
                        if (seg.id == id) seg.copy(text = text) else seg
                    }
                )
            )
        }
    }

    fun removeTimedSubtitle(id: String) {
        _uiState.update { state ->
            state.copy(
                config = state.config.copy(
                    timedSubtitles = state.config.timedSubtitles.filterNot { it.id == id }
                )
            )
        }
    }

    fun clearTimedSubtitles() {
        _uiState.update { it.copy(config = it.config.copy(timedSubtitles = emptyList())) }
    }

    fun generateAnimatedVideo() {
        val snapshot = _uiState.value
        val photoUri = snapshot.selectedPhotoUri
        if (photoUri == null) {
            _uiState.update { it.copy(snackbarMessage = "Pilih foto terlebih dahulu.") }
            return
        }
        if (snapshot.config.selectedEffects.isEmpty() &&
            snapshot.config.textOverlays.isEmpty() &&
            snapshot.config.timedSubtitles.isEmpty()
        ) {
            _uiState.update { it.copy(snackbarMessage = "Aktifkan minimal 1 efek animasi atau tambahkan teks.") }
            return
        }
        if (snapshot.isRendering) return

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRendering = true,
                    renderProgress = 0.02f,
                    exportedFilePath = null,
                    statusMessage = "Merender animasi efek ke video MP4 (${formatSec(snapshot.config.durationSeconds)})…",
                    statusIsError = false,
                )
            }
            try {
                val outFile = PhotoAnimatorRenderer.renderToMp4(
                    context = appContext,
                    photoUri = photoUri,
                    config = snapshot.config,
                    audioUri = snapshot.selectedAudioUri,
                    onProgress = { p ->
                        _uiState.update { it.copy(renderProgress = p.coerceIn(0f, 1f)) }
                    },
                )

                val thumbPath = snapshot.previewBitmap?.let { bmp ->
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 76, baos)
                    ImageHelper.saveCompressedThumbnail(appContext, baos.toByteArray())
                }

                val fxSummary = snapshot.config.selectedEffects
                    .take(3)
                    .joinToString(" + ") { it.label }
                    .ifBlank { "Teks Animasi" }
                val title = "Animasi: $fxSummary"
                val fileUriStr = Uri.fromFile(outFile).toString()

                val historyId = "anim-${System.currentTimeMillis()}"
                withContext(Dispatchers.IO) {
                    VideoDraftStore.savePhotoFxDraft(
                        context = appContext,
                        historyId = historyId,
                        photoUri = photoUri,
                        photoName = snapshot.selectedPhotoName,
                        audioUri = snapshot.selectedAudioUri,
                        audioName = snapshot.selectedAudioName,
                        config = snapshot.config,
                    )
                }
                historyRepo.upsert(
                    VideoHistoryEntity(
                        id = historyId,
                        userId = userId,
                        title = title.take(72),
                        modelLabel = "Animasi Foto (${snapshot.config.selectedEffects.size} Efek · ${formatSec(snapshot.config.durationSeconds)})",
                        mode = "image",
                        status = VideoStatus.COMPLETED.wireValue,
                        progressText = "SELESAI · 0 KREDIT",
                        thumbPath = thumbPath,
                        taskId = null,
                        videoUrl = fileUriStr,
                        error = null,
                        createdAt = System.currentTimeMillis(),
                        completedAt = System.currentTimeMillis(),
                        estimatedCredits = 0,
                    )
                )

                _uiState.update {
                    it.copy(
                        isRendering = false,
                        renderProgress = 1f,
                        exportedFilePath = outFile.absolutePath,
                        statusMessage = "Video animasi selesai dibuat & tersimpan ke Galeri!",
                        statusIsError = false,
                        snackbarMessage = "Video animasi siap diunduh!",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRendering = false,
                        statusMessage = "Gagal merender video: ${e.localizedMessage ?: "kesalahan"}",
                        statusIsError = true,
                    )
                }
            }
        }
    }

    fun cancelRender() {
        renderJob?.cancel()
        _uiState.update {
            it.copy(
                isRendering = false,
                renderProgress = 0f,
                statusMessage = "Proses pembuatan animasi dibatalkan.",
                statusIsError = false,
            )
        }
    }

    fun hasSavedDraft(item: VideoHistoryEntity): Boolean =
        VideoDraftStore.hasDraft(appContext, item.id)

    fun clearExportResult() {
        _uiState.update {
            it.copy(
                exportedFilePath = null,
                renderProgress = 0f,
                statusMessage = null,
            )
        }
    }

    fun restoreFromHistory(item: VideoHistoryEntity) {
        viewModelScope.launch {
            val draft = withContext(Dispatchers.IO) {
                VideoDraftStore.loadDraft(appContext, item.id) as? SavedVideoDraft.PhotoFx
            }
            val candidateUri = when {
                draft?.photoFilePath != null && File(draft.photoFilePath).exists() ->
                    Uri.fromFile(File(draft.photoFilePath))
                !draft?.photoUriString.isNullOrBlank() ->
                    Uri.parse(draft!!.photoUriString)
                !item.thumbPath.isNullOrBlank() && File(item.thumbPath).exists() ->
                    Uri.fromFile(File(item.thumbPath))
                else -> null
            }
            val bmp = candidateUri?.let { decodePreviewBitmap(it) }
            if (draft != null) {
                val restoredAudioUri = draft.audioUriString?.let { Uri.parse(it) }
                _uiState.update {
                    it.copy(
                        selectedPhotoUri = candidateUri,
                        selectedPhotoName = draft.photoName,
                        previewBitmap = bmp,
                        selectedAudioUri = restoredAudioUri,
                        selectedAudioName = draft.audioName,
                        config = draft.config,
                        textDraft = TextOverlayDraft(),
                        exportedFilePath = null,
                        renderProgress = 0f,
                        statusMessage = "Proyek animasi dimuat ulang. Edit efek atau teks lalu Ekspor lagi!",
                        statusIsError = false,
                        snackbarMessage = "Animasi dimuat ulang untuk diedit!",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedPhotoUri = candidateUri,
                        selectedPhotoName = item.title,
                        previewBitmap = bmp,
                        exportedFilePath = null,
                        renderProgress = 0f,
                        statusMessage = if (bmp != null) {
                            "Foto proyek dimuat ulang. Atur efek & teks lalu Ekspor lagi!"
                        } else {
                            "Pilih foto untuk melanjutkan pengeditan animasi."
                        },
                        statusIsError = false,
                        snackbarMessage = "Dibuka di Studio Animasi untuk diedit lagi.",
                    )
                }
            }
        }
    }

    fun downloadExportedVideo() {
        val path = _uiState.value.exportedFilePath ?: return
        val res = VideoDownloader.saveOrDownloadVideo(
            context = appContext,
            videoUrl = "file://$path",
            title = "animasi-foto",
        )
        _uiState.update {
            it.copy(
                snackbarMessage = res.getOrElse { err ->
                    "Gagal menyimpan: ${err.message ?: "kesalahan"}"
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

    private suspend fun extractMediaDurationSeconds(uri: Uri): Float? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: return@withContext null
            val rawSec = (ms / 100f).roundToInt() / 10f
            rawSec.coerceIn(3f, 180f)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
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

    private suspend fun decodePreviewBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxDim = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 960) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val raw = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            } ?: return@runCatching null

            val rotation = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            if (rotation == 0f) raw else {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        }.getOrNull()
    }

    private fun formatSec(sec: Float): String {
        val rounded = (sec * 10f).roundToInt() / 10f
        return if (rounded % 1f == 0f) "${rounded.toInt()} detik" else String.format(Locale.US, "%.1f detik", rounded)
    }

    companion object {
        fun factory(application: Application, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PhotoEffectsViewModel(application, userId) as T
                }
            }
    }
}
