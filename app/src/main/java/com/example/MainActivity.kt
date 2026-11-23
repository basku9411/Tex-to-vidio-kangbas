package com.example

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.AuthRepository
import com.example.model.AnimationStudioMode
import com.example.model.AppTab
import com.example.ui.components.NoticeDialog
import com.example.ui.components.QrisDialog
import com.example.ui.components.TopHeader
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PhotoEffectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CosmicBg
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicGlow
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicNoticeBg
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.KangbasTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.AutoCaptionViewModel
import com.example.viewmodel.KangbasViewModel
import com.example.viewmodel.PhotoEffectsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            KangbasTheme {
                KangbasRootGate()
            }
        }
    }
}

@Composable
private fun KangbasRootGate() {
    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val application = remember(context) { context.applicationContext as Application }
    val authRepository = remember(application) { AuthRepository(application) }
    val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(
        initialValue = authRepository.currentUser
    )
    val scope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var autoSignInAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null && !autoSignInAttempted) {
            autoSignInAttempted = true
            isSigningIn = true
            runCatching { authRepository.attemptAutoSignIn(activityContext) }
            isSigningIn = false
        }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(
            isSigningIn = isSigningIn,
            errorMessage = authError,
            onGoogleSignInClick = {
                if (isSigningIn) return@AuthScreen
                isSigningIn = true
                authError = null
                scope.launch {
                    val result = authRepository.signInWithGoogle(activityContext)
                    isSigningIn = false
                    result.onFailure { err ->
                        authError = err.localizedMessage ?: "Gagal masuk dengan akun Google."
                    }
                }
            },
        )
    } else {
        key(signedInUser.uid) {
            val userId = signedInUser.uid
            val kangbasVm: KangbasViewModel = viewModel(
                key = "kangbas_$userId",
                factory = KangbasViewModel.factory(application, userId),
            )
            val photoFxVm: PhotoEffectsViewModel = viewModel(
                key = "photofx_$userId",
                factory = PhotoEffectsViewModel.factory(application, userId),
            )
            val autoCaptionVm: AutoCaptionViewModel = viewModel(
                key = "autocap_$userId",
                factory = AutoCaptionViewModel.factory(application, userId),
            )

            KangbasApp(
                userEmail = signedInUser.email.orEmpty(),
                userDisplayName = signedInUser.displayName.orEmpty(),
                onSignOut = {
                    scope.launch {
                        authError = null
                        authRepository.signOut(activityContext)
                    }
                },
                viewModel = kangbasVm,
                photoEffectsViewModel = photoFxVm,
                autoCaptionViewModel = autoCaptionVm,
            )
        }
    }
}

@Composable
fun KangbasApp(
    userEmail: String,
    userDisplayName: String,
    onSignOut: () -> Unit,
    viewModel: KangbasViewModel,
    photoEffectsViewModel: PhotoEffectsViewModel,
    autoCaptionViewModel: AutoCaptionViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val photoFxState by photoEffectsViewModel.uiState.collectAsStateWithLifecycle()
    val captionState by autoCaptionViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(photoFxState.snackbarMessage) {
        val msg = photoFxState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            photoEffectsViewModel.clearSnackbar()
        }
    }
    LaunchedEffect(captionState.snackbarMessage) {
        val msg = captionState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            autoCaptionViewModel.clearSnackbar()
        }
    }

    if (state.currentTab != AppTab.CREATE) {
        BackHandler {
            viewModel.selectTab(AppTab.CREATE)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBg)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicGlow.copy(alpha = 0.52f), Color.Transparent),
                    startY = 0f,
                    endY = 760f,
                )
            ),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CosmicNoticeBg,
                        contentColor = CosmicCreditText,
                    )
                }
            },
            topBar = {
                TopHeader(
                    totalCredits = state.totalCredits,
                    keyCount = state.keyPool.size,
                    validKeyCount = state.keyBalances.count { it.balance != null },
                    isRefreshing = state.isRefreshingCredit,
                    onRefreshClick = { viewModel.refreshCredit(showFailureNotice = true) },
                    onQrisClick = { viewModel.openQris() },
                )
            },
            bottomBar = {
                if (!useRail) {
                    Column(modifier = Modifier.background(CosmicSurface)) {
                        HorizontalDivider(color = CosmicBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = CosmicSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.navigationBarsPadding(),
                        ) {
                            TabNavItems().forEach { item ->
                                val selected = state.currentTab == item.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(item.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        indicatorColor = CosmicViolet,
                                        unselectedIconColor = CosmicMuted,
                                        unselectedTextColor = CosmicMuted,
                                    ),
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CosmicSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding(),
                    ) {
                        TabNavItems().forEach { item ->
                            val selected = state.currentTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = CosmicViolet,
                                    unselectedIconColor = CosmicMuted,
                                    unselectedTextColor = CosmicMuted,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }

                val isStudioTab = state.currentTab == AppTab.PHOTO_FX
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .let { mod ->
                            if (isStudioTab) mod
                            else mod
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod -> if (isStudioTab) mod.fillMaxHeight() else mod }
                            .widthIn(max = if (isStudioTab) 840.dp else 680.dp)
                            .let { mod ->
                                if (useRail) mod.navigationBarsPadding() else mod
                            },
                    ) {
                        when (state.currentTab) {
                            AppTab.CREATE -> CreateScreen(
                                state = state,
                                onModeChange = viewModel::setMode,
                                onModelSelect = viewModel::selectModel,
                                onPromptChange = viewModel::updatePrompt,
                                onRatioSelect = viewModel::selectRatio,
                                onResolutionSelect = viewModel::selectResolution,
                                onDurationSelect = viewModel::selectDuration,
                                onAudioToggle = viewModel::toggleAudio,
                                onImagePicked = viewModel::onImagePicked,
                                onClearImage = viewModel::clearSelectedImage,
                                onGenerateClick = viewModel::generateVideo,
                            )
                            AppTab.PHOTO_FX -> PhotoEffectsScreen(
                                state = photoFxState,
                                autoCaptionState = captionState,
                                galleryVideos = state.history,
                                onStudioModeChange = photoEffectsViewModel::setStudioMode,
                                onPhotoPicked = photoEffectsViewModel::onPhotoPicked,
                                onAudioPicked = photoEffectsViewModel::onAudioPicked,
                                onReferenceVideoPicked = photoEffectsViewModel::onReferenceVideoPicked,
                                onApplyReferenceVideoDuration = photoEffectsViewModel::applyReferenceVideoDuration,
                                onApplyAudioDuration = photoEffectsViewModel::applyAudioDuration,
                                onClearAudio = photoEffectsViewModel::clearAudio,
                                onToggleEffect = photoEffectsViewModel::toggleEffect,
                                onApplyQuickPreset = photoEffectsViewModel::applyQuickPreset,
                                onClearAllEffects = photoEffectsViewModel::clearAllEffects,
                                onDensitySelect = photoEffectsViewModel::setDensity,
                                onSpeedSelect = photoEffectsViewModel::setSpeed,
                                onColorThemeSelect = photoEffectsViewModel::setColorTheme,
                                onRatioSelect = photoEffectsViewModel::setRatio,
                                onDurationSecondsChange = photoEffectsViewModel::setDurationSeconds,
                                onGenerateTimedSubtitles = photoEffectsViewModel::generateTimedSubtitlesFromAudio,
                                onSelectTimedPreset = photoEffectsViewModel::selectTimedSubtitlePreset,
                                onUpdateTimedSubtitle = photoEffectsViewModel::updateTimedSubtitleText,
                                onRemoveTimedSubtitle = photoEffectsViewModel::removeTimedSubtitle,
                                onClearTimedSubtitles = photoEffectsViewModel::clearTimedSubtitles,
                                onDraftTextChange = photoEffectsViewModel::updateDraftText,
                                onDraftFontSelect = photoEffectsViewModel::selectDraftFont,
                                onDraftColorSelect = photoEffectsViewModel::selectDraftColor,
                                onDraftStyleSelect = photoEffectsViewModel::selectDraftStyle,
                                onDraftMotionSelect = photoEffectsViewModel::selectDraftMotion,
                                onDraftSizeChange = photoEffectsViewModel::setDraftSizeFraction,
                                onDraftYChange = photoEffectsViewModel::setDraftYFraction,
                                onSaveDraftText = photoEffectsViewModel::saveDraftTextOverlay,
                                onCancelEditText = photoEffectsViewModel::cancelEditingText,
                                onEditOverlay = photoEffectsViewModel::editTextOverlay,
                                onRemoveOverlay = photoEffectsViewModel::removeTextOverlay,
                                onGenerateClick = photoEffectsViewModel::generateAnimatedVideo,
                                onCancelRender = photoEffectsViewModel::cancelRender,
                                onDownloadResult = photoEffectsViewModel::downloadExportedVideo,
                                onOpenResultExternal = photoEffectsViewModel::openExportedExternal,
                                onClearExportResult = photoEffectsViewModel::clearExportResult,
                                onPickCaptionVideoUri = { uri -> autoCaptionViewModel.onVideoPicked(uri) },
                                onSelectCaptionHistoryVideo = autoCaptionViewModel::selectFromHistory,
                                onGenerateCaptions = autoCaptionViewModel::generateAutoCaptions,
                                onSelectCaptionPreset = autoCaptionViewModel::selectPreset,
                                onSelectCaptionPosition = autoCaptionViewModel::selectPosition,
                                onSelectCaptionSize = autoCaptionViewModel::selectSize,
                                onSelectCaptionColor = autoCaptionViewModel::selectColor,
                                onUpdateCaptionSegmentText = autoCaptionViewModel::updateSegmentText,
                                onShiftCaptionSegmentTime = autoCaptionViewModel::shiftSegmentTime,
                                onDeleteCaptionSegment = autoCaptionViewModel::deleteSegment,
                                onAddManualCaptionSegment = autoCaptionViewModel::addManualSegment,
                                onExportCaptionVideo = autoCaptionViewModel::exportVideoWithSubtitles,
                                onCancelCaptionExport = autoCaptionViewModel::cancelExport,
                                onDownloadCaptionResult = autoCaptionViewModel::downloadExportedVideo,
                                onOpenCaptionResultExternal = autoCaptionViewModel::openExportedExternal,
                                onClearCaptionExportResult = autoCaptionViewModel::clearExportResult,
                            )
                            AppTab.LIBRARY -> LibraryScreen(
                                history = state.history,
                                showClearConfirm = state.showClearHistoryConfirm,
                                onDownloadVideo = viewModel::downloadVideo,
                                onOpenVideoExternal = viewModel::openVideoExternal,
                                onReEditVideo = { item ->
                                    when {
                                        item.id.startsWith("anim-") || photoEffectsViewModel.hasSavedDraft(item) -> {
                                            photoEffectsViewModel.restoreFromHistory(item)
                                            photoEffectsViewModel.setStudioMode(AnimationStudioMode.PHOTO_ANIMATION)
                                            viewModel.selectTab(AppTab.PHOTO_FX)
                                        }
                                        item.id.startsWith("caption-") || autoCaptionViewModel.hasSavedDraft(item) -> {
                                            autoCaptionViewModel.restoreFromHistoryForEdit(item)
                                            photoEffectsViewModel.setStudioMode(AnimationStudioMode.AUTO_CAPTION)
                                            viewModel.selectTab(AppTab.PHOTO_FX)
                                        }
                                        else -> {
                                            viewModel.reEditCreateItem(item)
                                        }
                                    }
                                },
                                onDeleteVideo = viewModel::deleteHistoryItem,
                                onRequestClearHistory = viewModel::requestClearHistory,
                                onDismissClearHistory = viewModel::dismissClearHistory,
                                onConfirmClearHistory = viewModel::confirmClearHistory,
                            )
                            AppTab.SETTINGS -> SettingsScreen(
                                state = state,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                onSignOut = onSignOut,
                                onKeyInputChange = viewModel::updateKeyInput,
                                onSaveKeys = viewModel::saveApiKeys,
                                onToggleShowKeys = viewModel::toggleShowKeys,
                                onCheckBalance = { viewModel.refreshCredit(showFailureNotice = true) },
                                onSelectActiveKey = viewModel::selectActiveKey,
                                onRemoveKeyAt = viewModel::removeKeyAt,
                                onCopyAllKeys = viewModel::copyAllKeys,
                                onRequestDeleteAllKeys = viewModel::requestDeleteAllKeys,
                                onDismissDeleteAllKeys = viewModel::dismissDeleteAllKeys,
                                onConfirmDeleteAllKeys = viewModel::confirmDeleteAllKeys,
                            )
                        }
                    }
                }
            }
        }

        if (state.showQrisModal) {
            QrisDialog(
                remainingSeconds = state.qrisRemainingSeconds,
                onContinue = viewModel::closeQris,
            )
        }

        state.noticeModal?.let { notice ->
            NoticeDialog(
                notice = notice,
                onDismiss = viewModel::dismissNotice,
                onOpenQris = viewModel::openQris,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class TabNavItem(
    val tab: AppTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String,
)

@Composable
private fun TabNavItems(): List<TabNavItem> = listOf(
    TabNavItem(
        tab = AppTab.CREATE,
        label = stringResource(R.string.tab_create),
        icon = Icons.Rounded.MovieCreation,
        testTag = "nav_create_tab",
    ),
    TabNavItem(
        tab = AppTab.PHOTO_FX,
        label = stringResource(R.string.tab_photo_fx),
        icon = Icons.Rounded.AutoFixHigh,
        testTag = "nav_photo_fx_tab",
    ),
    TabNavItem(
        tab = AppTab.LIBRARY,
        label = stringResource(R.string.tab_library),
        icon = Icons.Rounded.VideoLibrary,
        testTag = "nav_library_tab",
    ),
    TabNavItem(
        tab = AppTab.SETTINGS,
        label = stringResource(R.string.tab_settings),
        icon = Icons.Rounded.Settings,
        testTag = "nav_settings_tab",
    ),
)
