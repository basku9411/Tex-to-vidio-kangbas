package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.VideoHistoryEntity
import com.example.model.VideoStatus
import com.example.ui.components.CosmicCard
import com.example.ui.components.VideoPlayerBox
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicCardTop
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorBg
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicInputBg
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicPillBg
import com.example.ui.theme.CosmicPillBorder
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.CosmicVioletDeep
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    history: List<VideoHistoryEntity>,
    showClearConfirm: Boolean,
    onDownloadVideo: (VideoHistoryEntity) -> Unit,
    onOpenVideoExternal: (VideoHistoryEntity) -> Unit,
    onReEditVideo: (VideoHistoryEntity) -> Unit,
    onDeleteVideo: (VideoHistoryEntity) -> Unit,
    onRequestClearHistory: () -> Unit,
    onDismissClearHistory: () -> Unit,
    onConfirmClearHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        CosmicCard {
            Text(
                text = stringResource(R.string.library_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.library_desc),
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.library_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag("library_empty_text"),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    history.forEachIndexed { idx, item ->
                        VideoHistoryCard(
                            item = item,
                            index = idx,
                            onDownload = { onDownloadVideo(item) },
                            onOpenExternal = { onOpenVideoExternal(item) },
                            onReEdit = { onReEditVideo(item) },
                            onDelete = { onDeleteVideo(item) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRequestClearHistory,
                enabled = history.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("clear_history_button"),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, CosmicBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_clear_history),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Text(
            text = stringResource(R.string.app_version_footer),
            style = MaterialTheme.typography.labelSmall,
            color = CosmicMuted.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 6.dp),
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = onDismissClearHistory,
            containerColor = CosmicCardTop,
            titleContentColor = CosmicText,
            textContentColor = CosmicMuted,
            title = { Text(text = stringResource(R.string.confirm_clear_history_title)) },
            text = { Text(text = stringResource(R.string.confirm_clear_history_desc)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmClearHistory,
                    modifier = Modifier.testTag("confirm_clear_history_button"),
                ) {
                    Text(text = stringResource(R.string.action_delete), color = CosmicErrorText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearHistory) {
                    Text(text = stringResource(R.string.action_cancel), color = CosmicCreditText)
                }
            },
        )
    }
}

@Composable
private fun VideoHistoryCard(
    item: VideoHistoryEntity,
    index: Int,
    onDownload: () -> Unit,
    onOpenExternal: () -> Unit,
    onReEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("id-ID"))
    }
    val status = VideoStatus.fromWire(item.status)
    val modeLabel = when (item.mode) {
        "caption" -> "Teks Otomatis"
        "image" -> "Image to Video"
        else -> "Text to Video"
    }
    var menuExpanded by remember(item.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_$index"),
        color = CosmicItemBg,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, CosmicBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { "Video AI" },
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicText,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.modelLabel} · $modeLabel · ${dateFormat.format(Date(item.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicMuted,
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_more_button_$index"),
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
                        containerColor = CosmicCardTop,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_download_video), color = CosmicText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = CosmicVioletBright,
                                )
                            },
                            enabled = !item.videoUrl.isNullOrBlank(),
                            onClick = {
                                menuExpanded = false
                                onDownload()
                            },
                            modifier = Modifier.testTag("menu_download_video_$index"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reedit_video), color = CosmicText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = null,
                                    tint = CosmicCreditText,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onReEdit()
                            },
                            modifier = Modifier.testTag("menu_reedit_video_$index"),
                        )
                        HorizontalDivider(color = CosmicBorder)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_video), color = CosmicErrorText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = CosmicErrorText,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            modifier = Modifier.testTag("menu_delete_video_$index"),
                        )
                    }
                }
            }

            item.thumbPath?.let { path ->
                val thumbFile = remember(path) { File(path) }
                if (thumbFile.exists()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = stringResource(R.string.cd_selected_photo_preview),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicInputBg),
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            when (status) {
                VideoStatus.QUEUED, VideoStatus.PROCESSING -> {
                    StatePill(text = "⏳ ${item.progressText.ifBlank { "SEDANG DIPROSES" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedCosmicProgressBar()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (!item.taskId.isNullOrBlank()) {
                            "Task ID: ${item.taskId}"
                        } else {
                            "Menyiapkan permintaan ke Kie.ai…"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicMuted,
                    )
                }
                VideoStatus.FAILED -> {
                    StatePill(
                        text = "Gagal diproses",
                        containerColor = CosmicErrorBg,
                        textColor = CosmicErrorText,
                        borderColor = CosmicErrorText.copy(alpha = 0.4f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.error ?: "Permintaan gagal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicErrorText,
                    )
                }
                VideoStatus.COMPLETED -> {
                    StatePill(text = "✓ Video selesai")
                    val videoUrl = item.videoUrl
                    if (!videoUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        VideoPlayerBox(videoUrl = videoUrl)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onDownload,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("download_video_button_$index"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicVioletBright,
                                    contentColor = CosmicOnViolet,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_download_video),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenExternal,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("open_video_button_$index"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CosmicBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CosmicSurfaceAlt,
                                    contentColor = CosmicText,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_open_video),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatePill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color = CosmicPillBg,
    textColor: androidx.compose.ui.graphics.Color = CosmicCreditText,
    borderColor: androidx.compose.ui.graphics.Color = CosmicPillBorder,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AnimatedCosmicProgressBar() {
    val transition = rememberInfiniteTransition(label = "kbprogress")
    val fraction by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "progress_width",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(CosmicBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Brush.horizontalGradient(listOf(CosmicVioletBright, CosmicVioletDeep))),
        )
    }
}
