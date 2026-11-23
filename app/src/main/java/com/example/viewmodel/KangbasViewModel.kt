package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApiKeyRepository
import com.example.data.local.SavedVideoDraft
import com.example.data.local.VideoDraftStore
import com.example.data.local.VideoHistoryEntity
import com.example.data.local.VideoHistoryRepository
import com.example.data.remote.KieApiClient
import com.example.data.remote.KieApiException
import com.example.model.AiModel
import com.example.model.ApiKeyBalance
import com.example.model.AppTab
import com.example.model.AspectRatioOption
import com.example.model.CreditCalculator
import com.example.model.DurationOption
import com.example.model.GenerationMode
import com.example.model.NoticeMessage
import com.example.model.PriceEstimate
import com.example.model.ResolutionOption
import com.example.model.VideoStatus
import com.example.util.ImageHelper
import com.example.util.SelectedImageData
import com.example.util.VideoDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class KangbasUiState(
    val currentTab: AppTab = AppTab.CREATE,
    val mode: GenerationMode = GenerationMode.TEXT,
    val selectedModel: AiModel = AiModel.SEEDANCE_V1_LITE_TEXT,
    val prompt: String = "",
    val ratio: AspectRatioOption = AspectRatioOption.LANDSCAPE,
    val resolution: ResolutionOption = ResolutionOption.R720,
    val duration: DurationOption = DurationOption.D15,
    val audioEnabled: Boolean = false,
    val selectedImage: SelectedImageData? = null,
    val isBusy: Boolean = false,
    val statusText: String = "Siap membuat video. Masukkan API Key Kie.ai di menu Pengaturan.",
    val statusIsError: Boolean = false,
    val keyPool: List<String> = emptyList(),
    val activeKeyIndex: Int = 0,
    val keyBalances: List<ApiKeyBalance> = emptyList(),
    val totalCredits: Double? = null,
    val isRefreshingCredit: Boolean = false,
    val keyInput: String = "",
    val showKeys: Boolean = false,
    val keyStatusText: String = "API Key belum disimpan.",
    val history: List<VideoHistoryEntity> = emptyList(),
    val showQrisModal: Boolean = false,
    val qrisRemainingSeconds: Int = 0,
    val noticeModal: NoticeMessage? = null,
    val snackbarMessage: String? = null,
    val showClearHistoryConfirm: Boolean = false,
    val showDeleteKeysConfirm: Boolean = false,
) {
    val priceEstimate: PriceEstimate
        get() = CreditCalculator.buildPriceEstimate(
            model = selectedModel,
            durationSeconds = duration.seconds,
            resolution = resolution,
            audio = audioEnabled,
        )
}

class KangbasViewModel(
    application: Application,
    private val userId: String,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiKeyRepo = ApiKeyRepository(appContext)
    private val historyRepo = VideoHistoryRepository(appContext, userId)
    private val apiClient = KieApiClient()

    private val _uiState = MutableStateFlow(KangbasUiState())
    val uiState: StateFlow<KangbasUiState> = _uiState.asStateFlow()

    private var initialStartupCheckDone = false

    init {
        viewModelScope.launch {
            apiKeyRepo.stateFlow.collect { stored ->
                val balancesList = stored.keyPool.map { key ->
                    ApiKeyBalance(
                        key = key,
                        balance = stored.cachedBalances[key],
                        error = stored.cachedErrors[key],
                    )
                }
                val computedTotal = CreditCalculator.sumValidCredits(balancesList)
                _uiState.update { state ->
                    val statusMsg = if (stored.keyPool.isEmpty()) {
                        "API Key belum disimpan."
                    } else if (state.keyStatusText == "API Key belum disimpan.") {
                        "✓ ${stored.keyPool.size} API Key tersimpan di perangkat ini."
                    } else {
                        state.keyStatusText
                    }
                    state.copy(
                        keyPool = stored.keyPool,
                        activeKeyIndex = stored.activeIndex,
                        keyBalances = balancesList,
                        totalCredits = computedTotal,
                        keyStatusText = statusMsg,
                    )
                }
                if (!initialStartupCheckDone) {
                    initialStartupCheckDone = true
                    if (stored.keyPool.isNotEmpty()) {
                        refreshCredit(showFailureNotice = false)
                    }
                }
            }
        }

        viewModelScope.launch {
            historyRepo.observeHistory().collect { items ->
                _uiState.update { it.copy(history = items) }
            }
        }

        viewModelScope.launch {
            delay(350L)
            openQris()
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setMode(mode: GenerationMode) {
        val msg = if (mode == GenerationMode.TEXT) {
            "Mode Text to Video dipilih."
        } else {
            "Mode Image to Video dipilih."
        }
        _uiState.update { it.copy(mode = mode, statusText = msg, statusIsError = false) }
    }

    fun selectModel(model: AiModel) {
        _uiState.update { state ->
            val nextResolution = if (!model.supports480p && state.resolution == ResolutionOption.R480) {
                ResolutionOption.R720
            } else {
                state.resolution
            }
            state.copy(selectedModel = model, resolution = nextResolution)
        }
    }

    fun updatePrompt(value: String) {
        if (value.length <= 5000) {
            _uiState.update { it.copy(prompt = value) }
        }
    }

    fun selectRatio(ratio: AspectRatioOption) {
        _uiState.update { it.copy(ratio = ratio) }
    }

    fun selectResolution(resolution: ResolutionOption) {
        _uiState.update { state ->
            if (resolution == ResolutionOption.R480 && !state.selectedModel.supports480p) {
                state
            } else {
                state.copy(resolution = resolution)
            }
        }
    }

    fun selectDuration(duration: DurationOption) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun toggleAudio(enabled: Boolean) {
        _uiState.update { it.copy(audioEnabled = enabled) }
    }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val result = ImageHelper.readAndValidate(appContext, uri)
            result.onSuccess { imageData ->
                _uiState.update {
                    it.copy(
                        selectedImage = imageData,
                        snackbarMessage = "Foto dipilih: ${imageData.fileName}",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(snackbarMessage = err.message ?: "Pilih foto JPG, PNG, atau WEBP maksimal 10 MB.")
                }
            }
        }
    }

    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImage = null) }
    }

    fun updateKeyInput(value: String) {
        _uiState.update { it.copy(keyInput = value) }
    }

    fun toggleShowKeys() {
        _uiState.update { it.copy(showKeys = !it.showKeys) }
    }

    fun saveApiKeys() {
        val raw = _uiState.value.keyInput
        val parsed = raw.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parsed.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Tempel minimal satu API Key.") }
            return
        }
        viewModelScope.launch {
            val (added, pool) = apiKeyRepo.addKeys(raw)
            val statusMsg = "$added kunci baru disimpan. Total: ${pool.size} kunci."
            val toastMsg = if (added > 0) "$added API Key berhasil disimpan." else "Semua kunci sudah ada."
            _uiState.update {
                it.copy(
                    keyInput = "",
                    keyStatusText = statusMsg,
                    snackbarMessage = toastMsg,
                )
            }
            refreshCredit(showFailureNotice = true)
        }
    }

    fun selectActiveKey(index: Int) {
        viewModelScope.launch {
            apiKeyRepo.setActiveIndex(index)
            _uiState.update {
                it.copy(snackbarMessage = "Kunci ${index + 1} dipilih.")
            }
            refreshCredit(showFailureNotice = false)
        }
    }

    fun removeKeyAt(index: Int) {
        viewModelScope.launch {
            apiKeyRepo.removeKeyAt(index)
            val state = apiKeyRepo.currentState()
            if (state.keyPool.isEmpty()) {
                _uiState.update {
                    it.copy(
                        keyStatusText = "API Key belum disimpan.",
                        snackbarMessage = "Kunci dihapus.",
                    )
                }
            } else {
                refreshCredit(showFailureNotice = false)
            }
        }
    }

    fun requestDeleteAllKeys() {
        _uiState.update { it.copy(showDeleteKeysConfirm = true) }
    }

    fun dismissDeleteAllKeys() {
        _uiState.update { it.copy(showDeleteKeysConfirm = false) }
    }

    fun confirmDeleteAllKeys() {
        viewModelScope.launch {
            apiKeyRepo.clearAllKeys()
            _uiState.update {
                it.copy(
                    showDeleteKeysConfirm = false,
                    keyStatusText = "Semua API Key dihapus.",
                    totalCredits = null,
                    keyBalances = emptyList(),
                    snackbarMessage = "Semua API Key telah dihapus.",
                )
            }
        }
    }

    fun copyAllKeys() {
        val pool = _uiState.value.keyPool
        if (pool.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Belum ada API Key.") }
            return
        }
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Kie.ai API Keys", pool.joinToString("\n")))
        _uiState.update { it.copy(snackbarMessage = "Semua API Key disalin.") }
    }

    fun refreshCredit(showFailureNotice: Boolean = true) {
        viewModelScope.launch {
            val state = apiKeyRepo.currentState()
            if (state.keyPool.isEmpty()) {
                _uiState.update {
                    it.copy(
                        totalCredits = null,
                        keyBalances = emptyList(),
                        keyStatusText = "Masukkan API Key Kie.ai di Pengaturan.",
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isRefreshingCredit = true) }
            try {
                val summary = apiClient.fetchAllKeyCredits(state.keyPool)
                apiKeyRepo.saveBalances(summary.keyBalances)
                val validCount = summary.keyBalances.count { it.balance != null }
                val total = summary.totalCredits
                if (total != null) {
                    val formatted = CreditCalculator.formatIdNumber(total)
                    val failedSuffix = if (summary.failedMessages.isNotEmpty()) {
                        " (${summary.failedMessages.size} kunci gagal)"
                    } else {
                        ""
                    }
                    _uiState.update {
                        it.copy(
                            totalCredits = total,
                            keyBalances = summary.keyBalances,
                            keyStatusText = "Total kredit semua kunci: $formatted kredit ($validCount/${state.keyPool.size} kunci aktif)$failedSuffix.",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            totalCredits = null,
                            keyBalances = summary.keyBalances,
                            keyStatusText = summary.failedMessages.joinToString(" | "),
                            noticeModal = if (showFailureNotice) {
                                NoticeMessage(
                                    icon = "🔑",
                                    title = "API Key tidak dapat digunakan",
                                    description = summary.failedMessages.joinToString("\n") +
                                        "\nPeriksa apakah kunci salah, kedaluwarsa, atau akses ditolak.",
                                )
                            } else {
                                it.noticeModal
                            },
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        keyStatusText = e.message ?: "Gagal memeriksa kredit.",
                        noticeModal = if (showFailureNotice) {
                            NoticeMessage(
                                icon = "⚠️",
                                title = "Gagal memeriksa kredit",
                                description = e.message ?: "Terjadi kesalahan saat menghubungi Kie.ai.",
                            )
                        } else {
                            it.noticeModal
                        },
                    )
                }
            } finally {
                _uiState.update { it.copy(isRefreshingCredit = false) }
            }
        }
    }

    fun generateVideo() {
        val snapshot = _uiState.value
        if (snapshot.isBusy) return

        val prompt = snapshot.prompt.trim()
        if (prompt.length < 2) {
            _uiState.update { it.copy(snackbarMessage = "Tulis deskripsi video terlebih dahulu.") }
            return
        }
        if (snapshot.mode == GenerationMode.IMAGE && snapshot.selectedImage == null) {
            _uiState.update { it.copy(snackbarMessage = "Pilih foto untuk Image to Video.") }
            return
        }
        if (snapshot.keyPool.isEmpty()) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Masukkan API Key Kie.ai di Pengaturan.",
                    currentTab = AppTab.SETTINGS,
                )
            }
            return
        }

        val model = snapshot.selectedModel
        val cost = snapshot.priceEstimate.credits
        if (cost == null) {
            _uiState.update {
                it.copy(
                    noticeModal = NoticeMessage(
                        icon = "⚠️",
                        title = "Biaya belum diketahui",
                        description = "Pengaturan model ini belum memiliki estimasi harga yang terverifikasi.",
                    )
                )
            }
            return
        }

        if (model.requiresImage && snapshot.mode != GenerationMode.IMAGE) {
            _uiState.update {
                it.copy(
                    noticeModal = NoticeMessage(
                        icon = "🖼️",
                        title = "Pilih Image to Video",
                        description = "Model ini memerlukan foto. Aktifkan mode Image to Video dan pilih foto.",
                    )
                )
            }
            return
        }

        if (snapshot.mode == GenerationMode.IMAGE && model.textOnly) {
            _uiState.update {
                it.copy(
                    noticeModal = NoticeMessage(
                        icon = "ℹ️",
                        title = "Pilih model Image to Video",
                        description = "Model yang dipilih hanya mendukung Text to Video. Pilih model Image di daftar.",
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    statusText = "Memeriksa saldo API Key...",
                    statusIsError = false,
                )
            }

            var currentHistoryItem: VideoHistoryEntity? = null
            try {
                // Ensure we have up-to-date balances across all keys
                var balances = _uiState.value.keyBalances
                val pool = apiKeyRepo.currentState().keyPool
                if (balances.size != pool.size || balances.all { it.balance == null }) {
                    val summary = apiClient.fetchAllKeyCredits(pool)
                    apiKeyRepo.saveBalances(summary.keyBalances)
                    balances = summary.keyBalances
                    _uiState.update {
                        it.copy(
                            keyBalances = balances,
                            totalCredits = summary.totalCredits,
                        )
                    }
                }

                val totalCredits = CreditCalculator.sumValidCredits(balances)
                if (totalCredits != null && totalCredits < cost) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = "Kredit total tidak mencukupi.",
                            statusIsError = true,
                            noticeModal = NoticeMessage(
                                icon = "💳",
                                title = "Kredit tidak cukup",
                                description = "Total kredit semua API Key adalah ${CreditCalculator.formatIdNumber(totalCredits)} kredit, sedangkan perkiraan biaya ${ceil(cost).toInt()} kredit. Pilih model yang lebih murah atau durasi lebih pendek.",
                            ),
                        )
                    }
                    return@launch
                }

                // Order candidate keys: start with active/sufficient keys first, then unknown balance, then others
                val activeIdx = apiKeyRepo.currentState().activeIndex.coerceIn(0, pool.lastIndex)
                val rotated = pool.indices.map { offset -> pool[(activeIdx + offset) % pool.size] }
                val balanceByKey = balances.associateBy { it.key }
                val sufficientKeys = rotated.filter { (balanceByKey[it]?.balance ?: -1.0) >= cost }
                val unknownKeys = rotated.filter { balanceByKey[it]?.balance == null && balanceByKey[it]?.error == null }
                val remainingValidKeys = rotated.filter { it !in sufficientKeys && it !in unknownKeys }

                if (totalCredits != null && sufficientKeys.isEmpty() && unknownKeys.isEmpty()) {
                    val maxSingle = balances.mapNotNull { it.balance }.maxOrNull() ?: 0.0
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = "Saldo per kunci tidak cukup untuk ${ceil(cost).toInt()} kredit.",
                            statusIsError = true,
                            noticeModal = NoticeMessage(
                                icon = "💳",
                                title = "Saldo per API Key tidak cukup",
                                description = "Total kredit semua kunci ${CreditCalculator.formatIdNumber(totalCredits)} kredit, namun setiap video dibayar dari satu kunci. Saldo tertinggi satu kunci saat ini ${CreditCalculator.formatIdNumber(maxSingle)} kredit (butuh ${ceil(cost).toInt()} kredit).",
                            ),
                        )
                    }
                    return@launch
                }

                val candidateKeys = (sufficientKeys + unknownKeys + remainingValidKeys).distinct()
                val thumbPath = if (snapshot.mode == GenerationMode.IMAGE && snapshot.selectedImage != null) {
                    ImageHelper.saveCompressedThumbnail(appContext, snapshot.selectedImage.bytes)
                } else {
                    null
                }

                val item = VideoHistoryEntity(
                    id = "local-${System.currentTimeMillis()}",
                    userId = userId,
                    title = prompt.take(72),
                    modelLabel = model.label,
                    mode = snapshot.mode.wireValue,
                    status = VideoStatus.QUEUED.wireValue,
                    progressText = "MENYIAPKAN VIDEO",
                    thumbPath = thumbPath,
                    taskId = null,
                    videoUrl = null,
                    error = null,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null,
                    estimatedCredits = ceil(cost).toInt(),
                )
                currentHistoryItem = item
                VideoDraftStore.saveCreateAiDraft(
                    context = appContext,
                    historyId = item.id,
                    mode = snapshot.mode,
                    model = model,
                    prompt = prompt,
                    ratio = snapshot.ratio,
                    resolution = snapshot.resolution,
                    duration = snapshot.duration,
                    audioEnabled = snapshot.audioEnabled,
                )
                historyRepo.upsert(item)

                _uiState.update {
                    it.copy(
                        currentTab = AppTab.LIBRARY,
                        statusText = "Menyiapkan proses video… Galeri menampilkan status secara langsung.",
                        statusIsError = false,
                    )
                }

                val (usedKey, taskId) = executeWithKeyFailover(candidateKeys) { key ->
                    val uploadedUrl = if (snapshot.mode == GenerationMode.IMAGE && snapshot.selectedImage != null) {
                        _uiState.update { it.copy(statusText = "Mengunggah foto ke Kie.ai...") }
                        apiClient.uploadImage(
                            bytes = snapshot.selectedImage.bytes,
                            fileName = snapshot.selectedImage.fileName,
                            mimeType = snapshot.selectedImage.mimeType,
                            apiKey = key,
                        )
                    } else {
                        null
                    }

                    val input = apiClient.buildTaskInput(
                        model = model,
                        prompt = prompt,
                        aspectRatio = snapshot.ratio.wireValue,
                        resolution = snapshot.resolution,
                        durationSeconds = snapshot.duration.seconds,
                        audio = snapshot.audioEnabled,
                        imageUrl = uploadedUrl,
                    )

                    val processingItem = item.copy(
                        status = VideoStatus.PROCESSING.wireValue,
                        progressText = "SEDANG DIPROSES",
                    )
                    currentHistoryItem = processingItem
                    historyRepo.upsert(processingItem)
                    _uiState.update { it.copy(statusText = "Mengirim permintaan ke Kie.ai…") }

                    apiClient.createTask(model = model, input = input, apiKey = key)
                }

                apiKeyRepo.setActiveKey(usedKey)

                val acceptedItem = (currentHistoryItem ?: item).copy(
                    status = VideoStatus.PROCESSING.wireValue,
                    taskId = taskId,
                    progressText = "VIDEO SEDANG DIBUAT",
                )
                currentHistoryItem = acceptedItem
                historyRepo.upsert(acceptedItem)
                _uiState.update {
                    it.copy(statusText = "Permintaan diterima. Task ID: $taskId\nVideo sedang dibuat…")
                }

                val videoUrl = pollTaskUntilComplete(taskId = taskId, apiKey = usedKey, baseItem = acceptedItem)
                val completedItem = acceptedItem.copy(
                    status = VideoStatus.COMPLETED.wireValue,
                    progressText = "SELESAI",
                    videoUrl = videoUrl,
                    completedAt = System.currentTimeMillis(),
                )
                currentHistoryItem = completedItem
                historyRepo.upsert(completedItem)

                _uiState.update {
                    it.copy(
                        statusText = "✓ Video berhasil dibuat! Hasilnya sudah masuk Galeri.",
                        statusIsError = false,
                        snackbarMessage = "Video selesai dibuat!",
                    )
                }
                refreshCredit(showFailureNotice = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Pembuatan video gagal."
                currentHistoryItem?.let { failedBase ->
                    if (failedBase.status != VideoStatus.COMPLETED.wireValue) {
                        historyRepo.upsert(
                            failedBase.copy(
                                status = VideoStatus.FAILED.wireValue,
                                error = msg,
                            )
                        )
                    }
                }
                val notice = when {
                    Regex("402|credit|kredit|balance|saldo|insufficient", RegexOption.IGNORE_CASE)
                        .containsMatchIn(msg) -> NoticeMessage(
                        icon = "💳",
                        title = "Kredit tidak mencukupi",
                        description = "Kie.ai menolak permintaan. Periksa saldo dan biaya model.",
                    )
                    Regex("401|403|invalid|expired|unauthorized|forbidden|api.?key", RegexOption.IGNORE_CASE)
                        .containsMatchIn(msg) -> NoticeMessage(
                        icon = "🔑",
                        title = "API Key bermasalah",
                        description = "API Key ditolak atau tidak aktif. Periksa Pengaturan.",
                    )
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        statusText = "Gagal: $msg",
                        statusIsError = true,
                        noticeModal = notice ?: it.noticeModal,
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun <T> executeWithKeyFailover(
        candidateKeys: List<String>,
        block: suspend (String) -> T,
    ): Pair<String, T> {
        var lastError: Exception? = null
        for (key in candidateKeys) {
            try {
                return key to block(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: KieApiException) {
                lastError = e
                if (e.isRetryableWithAnotherKey()) {
                    continue
                }
                throw e
            } catch (e: Exception) {
                throw e
            }
        }
        val baseMsg = lastError?.message ?: "Semua API Key gagal digunakan"
        throw KieApiException(
            "$baseMsg · Kangbas sudah mencoba semua API Key yang tersimpan. Periksa saldo masing-masing kunci dan pastikan kunci berasal dari akun yang memiliki kredit."
        )
    }

    private suspend fun pollTaskUntilComplete(
        taskId: String,
        apiKey: String,
        baseItem: VideoHistoryEntity,
    ): String {
        var latestItem = baseItem
        repeat(90) {
            delay(5000L)
            val poll = apiClient.getRecordInfo(taskId, apiKey)
            val progressLabel = if (poll.progress.isNotBlank()) "PROSES ${poll.progress}" else "SEDANG DIPROSES"
            _uiState.update {
                it.copy(statusText = "Memproses video... ${poll.progress}\nTask: $taskId")
            }
            latestItem = latestItem.copy(progressText = progressLabel)
            historyRepo.upsert(latestItem)

            val doneStates = setOf("success", "succeeded", "completed")
            if (!poll.videoUrl.isNullOrBlank() || poll.state in doneStates) {
                return poll.videoUrl
                    ?: throw KieApiException("Tugas selesai tetapi tautan video tidak ditemukan. Buka riwayat tugas Kie.ai.")
            }
            val failStates = setOf("fail", "failed", "error")
            if (poll.state in failStates) {
                throw KieApiException(poll.errorMessage ?: "Pembuatan video gagal.")
            }
        }
        throw KieApiException("Waktu tunggu habis. Task ID: $taskId")
    }

    fun downloadVideo(item: VideoHistoryEntity) {
        val url = item.videoUrl ?: return
        val res = VideoDownloader.saveOrDownloadVideo(appContext, url, item.title)
        val msg = res.getOrElse { err ->
            "Gagal menyimpan video: ${err.message ?: "kesalahan"}"
        }
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun openVideoExternal(item: VideoHistoryEntity) {
        val url = item.videoUrl ?: return
        if (!VideoDownloader.openExternal(appContext, url)) {
            _uiState.update { it.copy(snackbarMessage = "Tidak ada aplikasi pemutar video yang ditemukan.") }
        }
    }

    fun deleteHistoryItem(item: VideoHistoryEntity) {
        viewModelScope.launch {
            historyRepo.deleteItem(item)
            _uiState.update { it.copy(snackbarMessage = "Video dihapus dari Galeri.") }
        }
    }

    fun reEditCreateItem(item: VideoHistoryEntity) {
        val draft = VideoDraftStore.loadDraft(appContext, item.id) as? SavedVideoDraft.CreateAi
        if (draft != null) {
            _uiState.update {
                it.copy(
                    currentTab = AppTab.CREATE,
                    mode = draft.mode,
                    selectedModel = draft.model,
                    prompt = draft.prompt,
                    ratio = draft.ratio,
                    resolution = draft.resolution,
                    duration = draft.duration,
                    audioEnabled = draft.audioEnabled,
                    snackbarMessage = "Pengaturan video AI dimuat ulang. Silakan edit dan buat lagi!",
                )
            }
        } else {
            val fallbackMode = if (item.mode == GenerationMode.IMAGE.wireValue) {
                GenerationMode.IMAGE
            } else {
                GenerationMode.TEXT
            }
            val matchedModel = AiModel.entries.firstOrNull { it.label == item.modelLabel }
            _uiState.update { state ->
                state.copy(
                    currentTab = AppTab.CREATE,
                    mode = fallbackMode,
                    selectedModel = matchedModel ?: state.selectedModel,
                    prompt = item.title,
                    snackbarMessage = "Prompt video dimuat ulang ke Buat Video untuk diedit lagi.",
                )
            }
        }
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearHistoryConfirm = true) }
    }

    fun dismissClearHistory() {
        _uiState.update { it.copy(showClearHistoryConfirm = false) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            historyRepo.clearAll()
            _uiState.update {
                it.copy(
                    showClearHistoryConfirm = false,
                    snackbarMessage = "Riwayat video dihapus.",
                )
            }
        }
    }

    fun openQris() {
        _uiState.update {
            it.copy(
                showQrisModal = true,
                qrisRemainingSeconds = 0,
                noticeModal = null,
            )
        }
    }

    fun closeQris() {
        _uiState.update { it.copy(showQrisModal = false) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(noticeModal = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        fun factory(application: Application, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return KangbasViewModel(application, userId) as T
                }
            }
    }
}
