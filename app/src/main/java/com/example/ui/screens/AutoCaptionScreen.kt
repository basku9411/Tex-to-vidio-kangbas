package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.local.VideoHistoryEntity
import com.example.engine.SubtitleCanvasDrawer
import com.example.model.SubtitleColorOverride
import com.example.model.SubtitlePosition
import com.example.model.SubtitleSizeOption
import com.example.model.SubtitleStylePreset
import com.example.model.VideoStatus
import com.example.ui.components.CosmicCard
import com.example.ui.components.VideoPlayerBox
import com.example.ui.components.cosmicInputColors
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorBg
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicInputBg
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicPillBg
import com.example.ui.theme.CosmicPillBorder
import com.example.ui.theme.CosmicSuccess
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletActiveText
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.CosmicVioletDeep
import com.example.viewmodel.AutoCaptionUiState
import com.example.viewmodel.TranscriptionUiState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoCaptionScreen(
    state: AutoCaptionUiState,
    galleryVideos: List<VideoHistoryEntity>,
    onPickVideoUri: (android.net.Uri?) -> Unit,
    onSelectHistoryVideo: (VideoHistoryEntity) -> Unit,
    onGenerateCaptions: () -> Unit,
    onSelectPreset: (SubtitleStylePreset) -> Unit,
    onSelectPosition: (SubtitlePosition) -> Unit,
    onSelectSize: (SubtitleSizeOption) -> Unit,
    onSelectColor: (SubtitleColorOverride) -> Unit,
    onUpdateSegmentText: (String, String) -> Unit,
    onShiftSegmentTime: (String, Long, Long) -> Unit,
    onDeleteSegment: (String) -> Unit,
    onAddManualSegment: () -> Unit,
    onExportVideo: () -> Unit,
    onCancelExport: () -> Unit,
    onDownloadResult: () -> Unit,
    onOpenResultExternal: () -> Unit,
    onClearExportResult: () -> Unit,
) {
    val context = LocalContext.current
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onPickVideoUri,
    )

    val localGalleryVideos = remember(galleryVideos) {
        galleryVideos.filter { item ->
            val u = item.videoUrl.orEmpty()
            item.status == VideoStatus.COMPLETED.wireValue && (u.startsWith("file://") || u.startsWith("/"))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        CosmicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auto_caption_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicText,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = CosmicPillBg,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, CosmicPillBorder),
                ) {
                    Text(
                        text = "AI AUDIO -> TEKS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CosmicCreditText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.auto_caption_desc),
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("auto_caption_pick_video_button"),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.selectedVideoUri == null) {
                        stringResource(R.string.action_pick_video)
                    } else {
                        "Ganti Video (${state.selectedVideoTitle})"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (localGalleryVideos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Atau ambil dari Galeri lokal:",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicMutedLabel,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    localGalleryVideos.take(8).forEach { item ->
                        AssistChip(
                            onClick = { onSelectHistoryVideo(item) },
                            label = {
                                Text(
                                    text = item.title.take(22),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = CosmicItemBg,
                                labelColor = CosmicCreditText,
                            ),
                        )
                    }
                }
            }

            // Live ExoPlayer + Real-time Styled Subtitle Overlay Preview
            state.selectedVideoUri?.let { videoUri ->
                Spacer(modifier = Modifier.height(12.dp))
                LiveSubtitledVideoPreview(
                    videoUri = videoUri,
                    videoAspect = (state.videoWidth.toFloat() / state.videoHeight.toFloat()).coerceIn(0.55f, 1.85f),
                    state = state,
                )

                Spacer(modifier = Modifier.height(12.dp))
                val isTranscribing = state.transcriptionState is TranscriptionUiState.Loading
                val gradientBrush = Brush.linearGradient(listOf(CosmicVioletBright, CosmicVioletDeep))
                Button(
                    onClick = onGenerateCaptions,
                    enabled = !isTranscribing && !state.isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .background(
                            brush = gradientBrush,
                            shape = RoundedCornerShape(12.dp),
                            alpha = if (isTranscribing || state.isExporting) 0.55f else 1f,
                        )
                        .testTag("auto_caption_transcribe_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = CosmicOnViolet,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = CosmicOnViolet,
                    ),
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.4.dp,
                            color = CosmicOnViolet,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MENDENGARKAN AUDIO…",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_generate_captions),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                when (val tState = state.transcriptionState) {
                    is TranscriptionUiState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = CosmicErrorBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CosmicErrorText.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = tState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicErrorText,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .testTag("auto_caption_ai_error"),
                            )
                        }
                    }
                    is TranscriptionUiState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ ${tState.count} potongan teks terdeteksi dari audio video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicSuccess,
                        )
                    }
                    else -> Unit
                }
            }
        }

        // Subtitle Style Selector Card ("Pilihan Bentuk Teks")
        CosmicCard {
            Text(
                text = stringResource(R.string.label_subtitle_styles),
                style = MaterialTheme.typography.titleMedium,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Geser untuk memilih gaya huruf, kotak latar, atau efek cahaya neon:",
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SubtitleStylePreset.entries.forEach { preset ->
                    val selected = preset == state.selectedPreset
                    val fontFamily = remember(preset.fontRes) { FontFamily(Font(preset.fontRes)) }
                    Surface(
                        modifier = Modifier
                            .width(142.dp)
                            .heightIn(min = 84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset) }
                            .testTag("subtitle_style_${preset.name.lowercase()}"),
                        color = if (selected) CosmicVioletActiveBg else CosmicItemBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) CosmicViolet else CosmicBorder,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        preset.boxColorHex?.let { Color(it.toInt()) }
                                            ?: Color(0xFF1A0D12)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = preset.previewSample,
                                    fontFamily = fontFamily,
                                    fontSize = 14.sp,
                                    color = Color(preset.textColorHex.toInt()),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) CosmicVioletActiveText else CosmicMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            StyleOptionChipsRow(
                title = "Posisi Teks",
                options = SubtitlePosition.entries,
                selected = state.selectedPosition,
                labelOf = { it.label },
                onSelect = onSelectPosition,
            )
            Spacer(modifier = Modifier.height(8.dp))
            StyleOptionChipsRow(
                title = "Ukuran Huruf",
                options = SubtitleSizeOption.entries,
                selected = state.selectedSize,
                labelOf = { it.label },
                onSelect = onSelectSize,
            )
            Spacer(modifier = Modifier.height(8.dp))
            StyleOptionChipsRow(
                title = "Warna Teks",
                options = SubtitleColorOverride.entries,
                selected = state.selectedColor,
                labelOf = { it.label },
                onSelect = onSelectColor,
            )
        }

        // Editable Segments & Export Card
        CosmicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Daftar Teks (${state.segments.size} baris)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmicText,
                )
                OutlinedButton(
                    onClick = onAddManualSegment,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_manual_segment_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Baris", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (state.segments.isEmpty()) {
                Text(
                    text = "Belum ada teks. Tekan \"Buat Teks Otomatis (AI)\" setelah memilih video, atau tekan \"Tambah Baris\" untuk menulis manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.segments.forEachIndexed { idx, seg ->
                        Surface(
                            color = CosmicItemBg,
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, CosmicBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${formatMs(seg.startMs)} → ${formatMs(seg.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicCreditText,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = { onShiftSegmentTime(seg.id, -200L, -200L) },
                                            contentPadding = PaddingValues(horizontal = 6.dp),
                                        ) {
                                            Text("-0.2s", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = { onShiftSegmentTime(seg.id, 200L, 200L) },
                                            contentPadding = PaddingValues(horizontal = 6.dp),
                                        ) {
                                            Text("+0.2s", style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(
                                            onClick = { onDeleteSegment(seg.id) },
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Hapus baris teks",
                                                tint = CosmicErrorText,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = seg.text,
                                    onValueChange = { onUpdateSegmentText(seg.id, it) },
                                    singleLine = false,
                                    maxLines = 3,
                                    colors = cosmicInputColors(),
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("segment_input_$idx"),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            val exportBrush = Brush.linearGradient(listOf(CosmicVioletBright, CosmicVioletDeep))
            Button(
                onClick = onExportVideo,
                enabled = !state.isExporting && state.selectedVideoUri != null && state.segments.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(
                        brush = exportBrush,
                        shape = RoundedCornerShape(12.dp),
                        alpha = if (!state.isExporting && state.selectedVideoUri != null && state.segments.isNotEmpty()) 1f else 0.5f,
                    )
                    .testTag("burn_captions_export_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CosmicOnViolet,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = CosmicOnViolet,
                ),
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.4.dp,
                        color = CosmicOnViolet,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MENEMPELKAN TEKS ${(state.exportProgress * 100).roundToInt()}%…",
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_burn_captions),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (state.isExporting) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { state.exportProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = CosmicVioletBright,
                    trackColor = CosmicBorder,
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onCancelExport,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_cancel), color = CosmicErrorText)
                }
            }

            state.exportError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, style = MaterialTheme.typography.bodySmall, color = CosmicErrorText)
            }

            state.exportedFilePath?.let { outPath ->
                var menuExpanded by remember(outPath) { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "✓ Video berteks berhasil dibuat!",
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("auto_caption_result_more_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.cd_video_options),
                                tint = CosmicCreditText,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = CosmicItemBg,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_download_video), color = CosmicText) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null, tint = CosmicVioletBright)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDownloadResult()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_reedit_video), color = CosmicText) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Replay, contentDescription = null, tint = CosmicCreditText)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onClearExportResult()
                                },
                            )
                            HorizontalDivider(color = CosmicBorder)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_video), color = CosmicErrorText) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = CosmicErrorText)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onClearExportResult()
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                VideoPlayerBox(videoUrl = "file://$outPath")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onDownloadResult,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("auto_caption_download_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicVioletBright,
                            contentColor = CosmicOnViolet,
                        ),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_download_video), style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onOpenResultExternal,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("auto_caption_open_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CosmicBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CosmicSurfaceAlt,
                            contentColor = CosmicText,
                        ),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_video), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LiveSubtitledVideoPreview(
    videoUri: android.net.Uri,
    videoAspect: Float,
    state: AutoCaptionUiState,
) {
    val context = LocalContext.current
    var currentPosMs by remember(videoUri) { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(60L)
        }
    }

    val typeface = remember(state.selectedPreset) {
        SubtitleCanvasDrawer.loadTypeface(context, state.selectedPreset)
    }

    val activeText = remember(currentPosMs, state.segments) {
        state.segments.firstOrNull { currentPosMs in it.startMs..it.endMs }?.text
            ?: if (state.segments.isEmpty()) "Contoh Tampilan Teks Otomatis" else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(videoAspect)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize(),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!activeText.isNullOrBlank()) {
                SubtitleCanvasDrawer.drawSubtitle(
                    canvas = drawContext.canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    rawText = activeText,
                    preset = state.selectedPreset,
                    position = state.selectedPosition,
                    sizeOption = state.selectedSize,
                    colorOverride = state.selectedColor,
                    typeface = typeface,
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> StyleOptionChipsRow(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = CosmicMutedLabel,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(opt) },
                label = { Text(text = labelOf(opt), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CosmicItemBg,
                    labelColor = CosmicMuted,
                    selectedContainerColor = CosmicVioletActiveBg,
                    selectedLabelColor = CosmicVioletActiveText,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = CosmicBorder,
                    selectedBorderColor = CosmicViolet,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalTenths = (ms.coerceAtLeast(0L) / 100L)
    val secs = totalTenths / 10L
    val tenths = totalTenths % 10L
    val mins = secs / 60L
    val remSecs = secs % 60L
    return String.format(Locale.US, "%d:%02d.%d", mins, remSecs, tenths)
}
