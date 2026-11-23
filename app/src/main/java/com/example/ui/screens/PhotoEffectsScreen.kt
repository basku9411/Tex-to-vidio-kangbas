package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.VideoHistoryEntity
import com.example.engine.EffectCanvasDrawer
import com.example.engine.SubtitleCanvasDrawer
import com.example.model.AnimationFontOption
import com.example.model.AnimationStudioMode
import com.example.model.AnimationTextColor
import com.example.model.AnimationTextMotion
import com.example.model.AnimationTextStyle
import com.example.model.EffectColorTheme
import com.example.model.EffectDensity
import com.example.model.EffectSpeed
import com.example.model.PhotoEffect
import com.example.model.PhotoTextOverlay
import com.example.model.PhotoVideoRatio
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import com.example.ui.components.CosmicCard
import com.example.ui.components.PhotoStudioToolbar
import com.example.ui.components.PhotoTextEditorCard
import com.example.ui.components.StudioAudioPanel
import com.example.ui.components.StudioCanvasPanel
import com.example.ui.components.StudioDurationPanel
import com.example.ui.components.StudioExportResultCard
import com.example.ui.components.StudioToolTab
import com.example.ui.components.formatTimecode
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCardTop
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletActiveText
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.CosmicVioletDeep
import com.example.viewmodel.AutoCaptionUiState
import com.example.viewmodel.PhotoEffectsUiState
import kotlin.math.roundToInt

@Composable
fun PhotoEffectsScreen(
    state: PhotoEffectsUiState,
    autoCaptionState: AutoCaptionUiState,
    galleryVideos: List<VideoHistoryEntity>,
    onStudioModeChange: (AnimationStudioMode) -> Unit,
    onPhotoPicked: (android.net.Uri?) -> Unit,
    onAudioPicked: (android.net.Uri?) -> Unit,
    onReferenceVideoPicked: (android.net.Uri?) -> Unit,
    onApplyReferenceVideoDuration: () -> Unit,
    onApplyAudioDuration: () -> Unit,
    onClearAudio: () -> Unit,
    onToggleEffect: (PhotoEffect) -> Unit,
    onApplyQuickPreset: (String) -> Unit,
    onClearAllEffects: () -> Unit,
    onDensitySelect: (EffectDensity) -> Unit,
    onSpeedSelect: (EffectSpeed) -> Unit,
    onColorThemeSelect: (EffectColorTheme) -> Unit,
    onRatioSelect: (PhotoVideoRatio) -> Unit,
    onDurationSecondsChange: (Float, String?) -> Unit,
    onGenerateTimedSubtitles: () -> Unit,
    onSelectTimedPreset: (SubtitleStylePreset) -> Unit,
    onUpdateTimedSubtitle: (String, String) -> Unit,
    onRemoveTimedSubtitle: (String) -> Unit,
    onClearTimedSubtitles: () -> Unit,
    onDraftTextChange: (String) -> Unit,
    onDraftFontSelect: (AnimationFontOption) -> Unit,
    onDraftColorSelect: (AnimationTextColor) -> Unit,
    onDraftStyleSelect: (AnimationTextStyle) -> Unit,
    onDraftMotionSelect: (AnimationTextMotion) -> Unit,
    onDraftSizeChange: (Float) -> Unit,
    onDraftYChange: (Float) -> Unit,
    onSaveDraftText: () -> Unit,
    onCancelEditText: () -> Unit,
    onEditOverlay: (PhotoTextOverlay) -> Unit,
    onRemoveOverlay: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onCancelRender: () -> Unit,
    onDownloadResult: () -> Unit,
    onOpenResultExternal: () -> Unit,
    onClearExportResult: () -> Unit,
    // Integrated Video Auto-Caption Callbacks
    onPickCaptionVideoUri: (android.net.Uri?) -> Unit,
    onSelectCaptionHistoryVideo: (VideoHistoryEntity) -> Unit,
    onGenerateCaptions: () -> Unit,
    onSelectCaptionPreset: (SubtitleStylePreset) -> Unit,
    onSelectCaptionPosition: (SubtitlePosition) -> Unit,
    onSelectCaptionSize: (SubtitleSizeOption) -> Unit,
    onSelectCaptionColor: (SubtitleColorOverride) -> Unit,
    onUpdateCaptionSegmentText: (String, String) -> Unit,
    onShiftCaptionSegmentTime: (String, Long, Long) -> Unit,
    onDeleteCaptionSegment: (String) -> Unit,
    onAddManualCaptionSegment: () -> Unit,
    onExportCaptionVideo: () -> Unit,
    onCancelCaptionExport: () -> Unit,
    onDownloadCaptionResult: () -> Unit,
    onOpenCaptionResultExternal: () -> Unit,
    onClearCaptionExportResult: () -> Unit,
) {
    val context = LocalContext.current
    val fontTypefaces = remember(context) {
        AnimationFontOption.entries.associateWith { opt ->
            runCatching { ResourcesCompat.getFont(context, opt.fontRes) }.getOrNull()
                ?: android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val subtitleTypeface = remember(state.config.subtitlePreset) {
        SubtitleCanvasDrawer.loadTypeface(context, state.config.subtitlePreset)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onPhotoPicked,
    )
    val refVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onReferenceVideoPicked,
    )
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = onAudioPicked,
    )

    var selectedTool by remember { mutableStateOf(StudioToolTab.EFFECTS) }
    var isPlaying by remember { mutableStateOf(true) }
    var previewTimeSec by remember { mutableFloatStateOf(0f) }

    val durationLimit = state.config.durationSeconds.coerceIn(3f, 180f)
    LaunchedEffect(isPlaying, durationLimit, state.studioMode) {
        if (!isPlaying || state.studioMode != AnimationStudioMode.PHOTO_ANIMATION) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNanos = now
                val next = previewTimeSec + dt
                previewTimeSec = if (next >= durationLimit) 0f else next
            }
        }
    }

    val livePreviewConfig = remember(state.config, state.textDraft) {
        val draftText = state.textDraft.text.trim()
        if (draftText.isEmpty()) {
            state.config
        } else {
            val draftOverlay = PhotoTextOverlay(
                id = state.textDraft.editingId ?: "draft-live",
                text = draftText,
                font = state.textDraft.font,
                sizeFraction = state.textDraft.sizeFraction,
                yFraction = state.textDraft.yFraction,
                color = state.textDraft.color,
                style = state.textDraft.style,
                motion = state.textDraft.motion,
            )
            val merged = if (state.textDraft.editingId != null) {
                state.config.textOverlays.map {
                    if (it.id == state.textDraft.editingId) draftOverlay else it
                }
            } else {
                state.config.textOverlays + draftOverlay
            }
            state.config.copy(textOverlays = merged)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
    ) {
        // Unified Animasi Studio Switcher: Animasi Foto | Teks Otomatis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StudioSubModeChip(
                label = stringResource(R.string.animasi_mode_photo),
                icon = Icons.Rounded.AutoFixHigh,
                selected = state.studioMode == AnimationStudioMode.PHOTO_ANIMATION,
                onClick = { onStudioModeChange(AnimationStudioMode.PHOTO_ANIMATION) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("animasi_mode_photo"),
            )
            StudioSubModeChip(
                label = stringResource(R.string.animasi_mode_caption),
                icon = Icons.Rounded.Subtitles,
                selected = state.studioMode == AnimationStudioMode.AUTO_CAPTION,
                onClick = { onStudioModeChange(AnimationStudioMode.AUTO_CAPTION) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("animasi_mode_caption"),
            )
        }

        if (state.studioMode == AnimationStudioMode.AUTO_CAPTION) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                AutoCaptionScreen(
                    state = autoCaptionState,
                    galleryVideos = galleryVideos,
                    onPickVideoUri = onPickCaptionVideoUri,
                    onSelectHistoryVideo = onSelectCaptionHistoryVideo,
                    onGenerateCaptions = onGenerateCaptions,
                    onSelectPreset = onSelectCaptionPreset,
                    onSelectPosition = onSelectCaptionPosition,
                    onSelectSize = onSelectCaptionSize,
                    onSelectColor = onSelectCaptionColor,
                    onUpdateSegmentText = onUpdateCaptionSegmentText,
                    onShiftSegmentTime = onShiftCaptionSegmentTime,
                    onDeleteSegment = onDeleteCaptionSegment,
                    onAddManualSegment = onAddManualCaptionSegment,
                    onExportVideo = onExportCaptionVideo,
                    onCancelExport = onCancelCaptionExport,
                    onDownloadResult = onDownloadCaptionResult,
                    onOpenResultExternal = onOpenCaptionResultExternal,
                    onClearExportResult = onClearCaptionExportResult,
                )
            }
            return
        }

        // 1. Studio Top Bar (Foto Picker + Ekspor Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 48.dp)
                    .testTag("photo_fx_pick_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicText,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.previewBitmap == null) "Pilih Foto" else state.selectedPhotoName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val exportBrush = Brush.linearGradient(listOf(CosmicVioletBright, CosmicVioletDeep))
            Button(
                onClick = onGenerateClick,
                enabled = !state.isRendering,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(
                        brush = exportBrush,
                        shape = RoundedCornerShape(10.dp),
                        alpha = if (state.isRendering) 0.55f else 1f,
                    )
                    .testTag("photo_fx_generate_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CosmicOnViolet,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = CosmicOnViolet,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (state.isRendering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CosmicOnViolet,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${(state.renderProgress * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // 2. PINNED Live Preview Stage
        val previewAspect = state.config.ratio.width.toFloat() / state.config.ratio.height.toFloat()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF050506))
                .border(1.dp, CosmicBorderHighlight.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .testTag("photo_fx_live_preview"),
            contentAlignment = Alignment.Center,
        ) {
            val boxRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1.5f
            val canvasModifier = if (previewAspect > boxRatio) {
                Modifier.fillMaxWidth().aspectRatio(previewAspect)
            } else {
                Modifier.fillMaxHeight().aspectRatio(previewAspect)
            }

            Box(
                modifier = canvasModifier.clickable(enabled = state.previewBitmap == null) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    EffectCanvasDrawer.drawFrame(
                        canvas = drawContext.canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        photoBitmap = state.previewBitmap,
                        config = livePreviewConfig,
                        timeSec = previewTimeSec,
                        fontTypefaces = fontTypefaces,
                        subtitleTypeface = subtitleTypeface,
                    )
                }
                if (state.previewBitmap == null && state.textDraft.text.isBlank()) {
                    Surface(
                        color = CosmicCardTop.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CosmicBorderHighlight),
                    ) {
                        Text(
                            text = "Tap di sini atau tombol Pilih Foto di atas",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicCreditText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // 3. Playback & Mini Timeline Scrubber Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(48.dp)
                    .background(CosmicSurfaceAlt, CircleShape)
                    .testTag("studio_play_pause_button"),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda pratinjau" else "Putar pratinjau",
                    tint = CosmicText,
                )
            }

            Slider(
                value = previewTimeSec.coerceIn(0f, durationLimit),
                onValueChange = {
                    isPlaying = false
                    previewTimeSec = it
                },
                valueRange = 0f..durationLimit,
                colors = SliderDefaults.colors(
                    thumbColor = CosmicVioletBright,
                    activeTrackColor = CosmicVioletBright,
                    inactiveTrackColor = CosmicBorder,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )

            Text(
                text = "${formatTimecode(previewTimeSec)} / ${formatTimecode(durationLimit)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CosmicCreditText,
            )
        }

        if (state.isRendering) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { state.renderProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = CosmicVioletBright,
                    trackColor = CosmicBorder,
                )
                TextButton(onClick = onCancelRender) {
                    Text(stringResource(R.string.action_cancel), color = CosmicErrorText)
                }
            }
        }

        // 4. Horizontal Tool Toolbar
        HorizontalDivider(color = CosmicBorder, thickness = 1.dp)
        PhotoStudioToolbar(
            selectedTab = selectedTool,
            effectCount = state.config.selectedEffects.size,
            textCount = state.config.textOverlays.size + state.config.timedSubtitles.size,
            hasAudio = state.selectedAudioUri != null,
            onSelectTab = { selectedTool = it },
        )
        HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

        // 5. Scrollable Bottom Tool Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .background(CosmicSurface.copy(alpha = 0.55f))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.exportedFilePath?.let { filePath ->
                StudioExportResultCard(
                    filePath = filePath,
                    onDownloadResult = onDownloadResult,
                    onOpenResultExternal = onOpenResultExternal,
                    onReEditResult = onClearExportResult,
                    onDeleteResult = onClearExportResult,
                )
            }

            if (state.statusMessage != null && state.statusIsError) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicErrorText,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            when (selectedTool) {
                StudioToolTab.EFFECTS -> {
                    CosmicCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Pilih Karakter & Efek (${state.config.selectedEffects.size} aktif · geser ↔)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = CosmicMutedLabel,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = onClearAllEffects,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Kosongkan", color = CosmicMuted)
                            }
                        }

                        val topRow = remember { PhotoEffect.entries.filterIndexed { idx, _ -> idx % 2 == 0 } }
                        val bottomRow = remember { PhotoEffect.entries.filterIndexed { idx, _ -> idx % 2 == 1 } }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(topRow, bottomRow).forEach { rowEffects ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowEffects.forEach { effect ->
                                        val selected = effect in state.config.selectedEffects
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) CosmicVioletActiveBg else CosmicItemBg,
                                            border = BorderStroke(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) CosmicViolet else CosmicBorder,
                                            ),
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .semantics { contentDescription = effect.label }
                                                .clickable(role = Role.Checkbox) { onToggleEffect(effect) }
                                                .testTag("fx_chip_${effect.name.lowercase()}"),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = effect.emoji, fontSize = 22.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kombinasi Cepat",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CosmicMutedLabel,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "doremi_birds" to "🎵 Not + Burung",
                                "dove_peace" to "🕊️ Merpati + Bulan",
                                "party_music" to "🎼 Musik + Konfeti",
                                "magic_night" to "✨ Malam Magis",
                                "sakura_dream" to "🌸 Sakura + Kupu",
                                "rain_cozy" to "🌧️ Hujan + Riak",
                            ).forEach { (key, label) ->
                                AssistChip(
                                    onClick = { onApplyQuickPreset(key) },
                                    label = { Text(label) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CosmicItemBg,
                                        labelColor = CosmicCreditText,
                                    ),
                                    modifier = Modifier.testTag("preset_$key"),
                                )
                            }
                        }
                    }
                }

                StudioToolTab.TEXT -> PhotoTextEditorCard(
                    draft = state.textDraft,
                    overlays = state.config.textOverlays,
                    hasAudio = state.selectedAudioUri != null,
                    audioName = state.selectedAudioName,
                    isTranscribingAudio = state.isTranscribingAudio,
                    audioTranscribeError = state.audioTranscribeError,
                    timedSubtitles = state.config.timedSubtitles,
                    subtitlePreset = state.config.subtitlePreset,
                    onGenerateTimedSubtitles = onGenerateTimedSubtitles,
                    onSelectTimedPreset = onSelectTimedPreset,
                    onUpdateTimedSubtitle = onUpdateTimedSubtitle,
                    onRemoveTimedSubtitle = onRemoveTimedSubtitle,
                    onClearTimedSubtitles = onClearTimedSubtitles,
                    onTextChange = onDraftTextChange,
                    onFontSelect = onDraftFontSelect,
                    onColorSelect = onDraftColorSelect,
                    onStyleSelect = onDraftStyleSelect,
                    onMotionSelect = onDraftMotionSelect,
                    onSizeChange = onDraftSizeChange,
                    onYChange = onDraftYChange,
                    onSaveClick = onSaveDraftText,
                    onCancelEdit = onCancelEditText,
                    onEditOverlay = onEditOverlay,
                    onRemoveOverlay = onRemoveOverlay,
                )

                StudioToolTab.CANVAS -> StudioCanvasPanel(
                    state = state,
                    onRatioSelect = onRatioSelect,
                    onDensitySelect = onDensitySelect,
                    onSpeedSelect = onSpeedSelect,
                    onColorThemeSelect = onColorThemeSelect,
                )

                StudioToolTab.AUDIO -> StudioAudioPanel(
                    state = state,
                    onPickAudioClick = { audioPicker.launch(arrayOf("audio/*")) },
                    onClearAudio = onClearAudio,
                    onApplyAudioDuration = onApplyAudioDuration,
                )

                StudioToolTab.DURATION -> StudioDurationPanel(
                    state = state,
                    onPickReferenceVideo = {
                        refVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onApplyReferenceVideoDuration = onApplyReferenceVideoDuration,
                    onApplyAudioDuration = onApplyAudioDuration,
                    onDurationSecondsChange = onDurationSecondsChange,
                )
            }
        }
    }
}

@Composable
private fun StudioSubModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        color = if (selected) CosmicVioletActiveBg else CosmicItemBg,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, if (selected) CosmicViolet else CosmicBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CosmicVioletActiveText else CosmicMuted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) CosmicVioletActiveText else CosmicMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
