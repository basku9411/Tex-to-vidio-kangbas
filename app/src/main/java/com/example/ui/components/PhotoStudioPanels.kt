package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.EffectColorTheme
import com.example.model.EffectDensity
import com.example.model.EffectSpeed
import com.example.model.PhotoVideoDuration
import com.example.model.PhotoVideoRatio
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicPillBg
import com.example.ui.theme.CosmicPillBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletActiveText
import com.example.ui.theme.CosmicVioletBright
import com.example.viewmodel.PhotoEffectsUiState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class StudioToolTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
) {
    EFFECTS("Efek", Icons.Rounded.AutoAwesome, "studio_tab_effects"),
    TEXT("Teks", Icons.Rounded.TextFields, "studio_tab_text"),
    CANVAS("Kanvas", Icons.Rounded.Crop, "studio_tab_canvas"),
    AUDIO("Audio", Icons.Rounded.MusicNote, "studio_tab_audio"),
    DURATION("Durasi", Icons.Rounded.Timer, "studio_tab_duration"),
}

@Composable
fun PhotoStudioToolbar(
    selectedTab: StudioToolTab,
    effectCount: Int,
    textCount: Int,
    hasAudio: Boolean,
    onSelectTab: (StudioToolTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StudioToolTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val badgeActive = when (tab) {
                StudioToolTab.EFFECTS -> effectCount > 0
                StudioToolTab.TEXT -> textCount > 0
                StudioToolTab.AUDIO -> hasAudio
                else -> false
            }
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = if (selected) CosmicVioletActiveBg else Color.Transparent,
                border = if (selected) BorderStroke(1.dp, CosmicViolet) else null,
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .clickable { onSelectTab(tab) }
                    .testTag(tab.testTag),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) CosmicVioletBright else CosmicMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        if (badgeActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xFFFFD670), RoundedCornerShape(99.dp)),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) CosmicVioletActiveText else CosmicMuted,
                    )
                }
            }
        }
    }
}

@Composable
fun StudioCanvasPanel(
    state: PhotoEffectsUiState,
    onRatioSelect: (PhotoVideoRatio) -> Unit,
    onDensitySelect: (EffectDensity) -> Unit,
    onSpeedSelect: (EffectSpeed) -> Unit,
    onColorThemeSelect: (EffectColorTheme) -> Unit,
) {
    CosmicCard {
        Text(
            text = "Pengaturan Kanvas & Partikel",
            style = MaterialTheme.typography.titleMedium,
            color = CosmicText,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StudioChipGroup(
            label = "Ukuran Kanvas / Video",
            options = PhotoVideoRatio.entries,
            selected = state.config.ratio,
            optionLabel = { it.label },
            onSelect = onRatioSelect,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StudioChipGroup(
            label = "Kepadatan Partikel",
            options = EffectDensity.entries,
            selected = state.config.density,
            optionLabel = { it.label },
            onSelect = onDensitySelect,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StudioChipGroup(
            label = "Kecepatan Gerak",
            options = EffectSpeed.entries,
            selected = state.config.speed,
            optionLabel = { it.label },
            onSelect = onSpeedSelect,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StudioChipGroup(
            label = "Warna Tema Efek",
            options = EffectColorTheme.entries,
            selected = state.config.colorTheme,
            optionLabel = { it.label },
            onSelect = onColorThemeSelect,
        )
    }
}

@Composable
fun StudioAudioPanel(
    state: PhotoEffectsUiState,
    onPickAudioClick: () -> Unit,
    onClearAudio: () -> Unit,
    onApplyAudioDuration: () -> Unit,
) {
    CosmicCard {
        Text(
            text = "Musik / Audio Latar",
            style = MaterialTheme.typography.titleMedium,
            color = CosmicText,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pilih file musik (AAC/M4A/MP3) untuk ikut tergabung ke video & menyamakan durasi otomatis.",
            style = MaterialTheme.typography.bodySmall,
            color = CosmicMuted,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onPickAudioClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("photo_fx_pick_audio_button"),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicText,
                ),
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.selectedAudioName ?: stringResource(R.string.action_pick_audio),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (state.selectedAudioUri != null) {
                IconButton(onClick = onClearAudio, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Hapus audio", tint = CosmicMuted)
                }
            }
        }
        state.detectedAudioDurationSec?.let { aSec ->
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = onApplyAudioDuration,
                label = { Text("🎵 Pakai Durasi Audio (${formatStudioSec(aSec)})") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = CosmicItemBg,
                    labelColor = CosmicCreditText,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioDurationPanel(
    state: PhotoEffectsUiState,
    onPickReferenceVideo: () -> Unit,
    onApplyReferenceVideoDuration: () -> Unit,
    onApplyAudioDuration: () -> Unit,
    onDurationSecondsChange: (Float, String?) -> Unit,
) {
    CosmicCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Durasi: ${formatStudioSec(state.config.durationSeconds)}",
                style = MaterialTheme.typography.titleMedium,
                color = CosmicText,
            )
            state.config.durationSourceLabel?.let { src ->
                Surface(
                    color = CosmicPillBg,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, CosmicPillBorder),
                ) {
                    Text(
                        text = src,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicCreditText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPickReferenceVideo,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("photo_fx_match_video_duration_button"),
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, CosmicBorderHighlight),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CosmicSurfaceAlt,
                contentColor = CosmicCreditText,
            ),
        ) {
            Icon(Icons.Rounded.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.referenceVideoDurationSec != null) {
                    "Samakan Ulang dari Video (${formatStudioSec(state.referenceVideoDurationSec)})"
                } else {
                    stringResource(R.string.action_match_video_duration)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (state.referenceVideoDurationSec != null || state.detectedAudioDurationSec != null) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.referenceVideoDurationSec?.let { vSec ->
                    AssistChip(
                        onClick = onApplyReferenceVideoDuration,
                        label = { Text("🎬 Durasi Video (${formatStudioSec(vSec)})") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CosmicItemBg,
                            labelColor = CosmicCreditText,
                        ),
                    )
                }
                state.detectedAudioDurationSec?.let { aSec ->
                    AssistChip(
                        onClick = onApplyAudioDuration,
                        label = { Text("🎵 Durasi Audio (${formatStudioSec(aSec)})") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CosmicItemBg,
                            labelColor = CosmicCreditText,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PhotoVideoDuration.entries.forEach { preset ->
                val isSelected = abs(state.config.durationSeconds - preset.seconds) < 0.25f
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationSecondsChange(preset.seconds.toFloat(), null) },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
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

        Slider(
            value = state.config.durationSeconds.coerceIn(3f, 120f),
            onValueChange = { onDurationSecondsChange((it * 2f).roundToInt() / 2f, "Manual") },
            valueRange = 3f..120f,
            colors = SliderDefaults.colors(
                thumbColor = CosmicVioletBright,
                activeTrackColor = CosmicVioletBright,
                inactiveTrackColor = CosmicBorder,
            ),
        )
    }
}

@Composable
fun StudioExportResultCard(
    filePath: String,
    onDownloadResult: () -> Unit,
    onOpenResultExternal: () -> Unit,
    onReEditResult: () -> Unit,
    onDeleteResult: () -> Unit,
) {
    var menuExpanded by remember(filePath) { mutableStateOf(false) }

    CosmicCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Video Animasi Siap Diunduh",
                style = MaterialTheme.typography.titleMedium,
                color = CosmicText,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("photo_fx_result_more_button"),
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
                            onReEditResult()
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
                            onDeleteResult()
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        VideoPlayerBox(videoUrl = "file://$filePath")
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
                    .testTag("photo_fx_download_button"),
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
                    .testTag("photo_fx_open_button"),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> StudioChipGroup(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(
        text = label,
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
                label = { Text(optionLabel(opt), style = MaterialTheme.typography.labelSmall) },
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

fun formatStudioSec(sec: Float): String {
    val rounded = (sec * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) "${rounded.toInt()} detik" else String.format(Locale.US, "%.1f detik", rounded)
}

fun formatTimecode(sec: Float): String {
    val totalTenths = (sec.coerceAtLeast(0f) * 10f).roundToInt()
    val mins = totalTenths / 600
    val secs = (totalTenths % 600) / 10
    val tenths = totalTenths % 10
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}
