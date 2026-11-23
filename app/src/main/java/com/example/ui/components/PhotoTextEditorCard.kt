package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationFontOption
import com.example.model.AnimationTextColor
import com.example.model.AnimationTextMotion
import com.example.model.AnimationTextStyle
import com.example.model.PhotoTextOverlay
import com.example.model.SubtitleSegment
import com.example.model.SubtitleStylePreset
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
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletActiveText
import com.example.ui.theme.CosmicVioletBright
import com.example.viewmodel.TextOverlayDraft
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoTextEditorCard(
    draft: TextOverlayDraft,
    overlays: List<PhotoTextOverlay>,
    hasAudio: Boolean,
    audioName: String?,
    isTranscribingAudio: Boolean,
    audioTranscribeError: String?,
    timedSubtitles: List<SubtitleSegment>,
    subtitlePreset: SubtitleStylePreset,
    onGenerateTimedSubtitles: () -> Unit,
    onSelectTimedPreset: (SubtitleStylePreset) -> Unit,
    onUpdateTimedSubtitle: (String, String) -> Unit,
    onRemoveTimedSubtitle: (String) -> Unit,
    onClearTimedSubtitles: () -> Unit,
    onTextChange: (String) -> Unit,
    onFontSelect: (AnimationFontOption) -> Unit,
    onColorSelect: (AnimationTextColor) -> Unit,
    onStyleSelect: (AnimationTextStyle) -> Unit,
    onMotionSelect: (AnimationTextMotion) -> Unit,
    onSizeChange: (Float) -> Unit,
    onYChange: (Float) -> Unit,
    onSaveClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditOverlay: (PhotoTextOverlay) -> Unit,
    onRemoveOverlay: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // AI Auto-Text from Background Audio Card
        CosmicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Subtitles,
                        contentDescription = null,
                        tint = CosmicVioletBright,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Teks Otomatis dari Audio",
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmicText,
                    )
                }
                Surface(
                    color = CosmicPillBg,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, CosmicPillBorder),
                ) {
                    Text(
                        text = "AI AUDIO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CosmicCreditText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasAudio) {
                    "Audio aktif: ${audioName ?: "Musik"}. AI akan mendeteksi ucapan/lirik dan menampilkannya sinkron di animasi."
                } else {
                    "Pilih musik/audio dulu di tab Audio jika ingin membuat teks lirik/ucapan otomatis dengan AI."
                },
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onGenerateTimedSubtitles,
                enabled = !isTranscribingAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("photo_fx_auto_text_button"),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicVioletBright,
                    contentColor = CosmicOnViolet,
                ),
            ) {
                if (isTranscribingAudio) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CosmicOnViolet,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mendeteksi Suara Audio…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Teks Otomatis dari Audio", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (!audioTranscribeError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CosmicErrorBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CosmicErrorText.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = audioTranscribeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicErrorText,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            if (timedSubtitles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Bentuk Teks Otomatis (${timedSubtitles.size} baris)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CosmicMutedLabel,
                    )
                    TextButton(
                        onClick = onClearTimedSubtitles,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Hapus Semua", color = CosmicErrorText, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubtitleStylePreset.entries.forEach { preset ->
                        val selected = preset == subtitlePreset
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTimedPreset(preset) },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CosmicItemBg,
                                labelColor = CosmicMuted,
                                selectedContainerColor = CosmicVioletActiveBg,
                                selectedLabelColor = CosmicVioletActiveText,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = CosmicBorder,
                                selectedBorderColor = CosmicViolet,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    timedSubtitles.forEach { seg ->
                        Surface(
                            color = CosmicItemBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CosmicBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${formatTimedMs(seg.startMs)} → ${formatTimedMs(seg.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicCreditText,
                                    )
                                    IconButton(
                                        onClick = { onRemoveTimedSubtitle(seg.id) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Hapus baris teks otomatis",
                                            tint = CosmicErrorText,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = seg.text,
                                    onValueChange = { onUpdateTimedSubtitle(seg.id, it) },
                                    maxLines = 2,
                                    colors = cosmicInputColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual Text Overlay Card
        CosmicCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextFields,
                    contentDescription = null,
                    tint = CosmicVioletBright,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Tambah Teks Manual",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmicText,
                )
            }
            Surface(
                color = CosmicPillBg,
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, CosmicPillBorder),
            ) {
                Text(
                    text = "${AnimationFontOption.entries.size} FONT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CosmicCreditText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tambahkan judul, lirik, atau catatan manual langsung di atas calon animasi dengan banyak pilihan font.",
            style = MaterialTheme.typography.bodySmall,
            color = CosmicMuted,
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = draft.text,
            onValueChange = onTextChange,
            label = { Text("Teks yang ingin ditampilkan") },
            placeholder = { Text("Contoh: Do Re Mi · Selamat Pagi!") },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicInputBg,
                unfocusedContainerColor = CosmicInputBg,
                focusedBorderColor = CosmicViolet,
                unfocusedBorderColor = CosmicBorder,
                focusedTextColor = CosmicText,
                unfocusedTextColor = CosmicText,
                focusedLabelColor = CosmicVioletBright,
                unfocusedLabelColor = CosmicMutedLabel,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("photo_fx_text_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Pilih Font (${AnimationFontOption.entries.size} gaya huruf)",
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
            AnimationFontOption.entries.forEach { fontOpt ->
                val selected = draft.font == fontOpt
                val fontFamily = remember(fontOpt) { FontFamily(Font(fontOpt.fontRes)) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) CosmicVioletActiveBg else CosmicItemBg,
                    border = BorderStroke(1.dp, if (selected) CosmicViolet else CosmicBorder),
                    modifier = Modifier
                        .width(126.dp)
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFontSelect(fontOpt) }
                        .testTag("font_card_${fontOpt.name.lowercase()}"),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Aa Bb 12",
                            fontFamily = fontFamily,
                            fontSize = 18.sp,
                            color = if (selected) CosmicVioletActiveText else CosmicText,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fontOpt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) CosmicCreditText else CosmicMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Warna Teks",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CosmicMutedLabel,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimationTextColor.entries.forEach { colorOpt ->
                val selected = draft.color == colorOpt
                val parsed = remember(colorOpt.hex) {
                    Color(android.graphics.Color.parseColor(colorOpt.hex))
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) CosmicVioletBright else CosmicBorder,
                            shape = CircleShape,
                        )
                        .clickable { onColorSelect(colorOpt) }
                        .padding(6.dp)
                        .testTag("text_color_${colorOpt.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parsed),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = colorOpt.label,
                                tint = if (parsed.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Gaya Teks",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CosmicMutedLabel,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimationTextStyle.entries.forEach { styleOpt ->
                val selected = draft.style == styleOpt
                FilterChip(
                    selected = selected,
                    onClick = { onStyleSelect(styleOpt) },
                    label = { Text(styleOpt.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CosmicItemBg,
                        labelColor = CosmicMuted,
                        selectedContainerColor = CosmicVioletActiveBg,
                        selectedLabelColor = CosmicVioletActiveText,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = CosmicBorder,
                        selectedBorderColor = CosmicViolet,
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gerak Teks",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = CosmicMutedLabel,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimationTextMotion.entries.forEach { motionOpt ->
                val selected = draft.motion == motionOpt
                FilterChip(
                    selected = selected,
                    onClick = { onMotionSelect(motionOpt) },
                    label = { Text(motionOpt.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CosmicItemBg,
                        labelColor = CosmicMuted,
                        selectedContainerColor = CosmicVioletActiveBg,
                        selectedLabelColor = CosmicVioletActiveText,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = CosmicBorder,
                        selectedBorderColor = CosmicViolet,
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ukuran Teks: ${(draft.sizeFraction * 100f).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = CosmicMutedLabel,
        )
        Slider(
            value = draft.sizeFraction,
            onValueChange = onSizeChange,
            valueRange = 0.035f..0.125f,
            colors = SliderDefaults.colors(
                thumbColor = CosmicVioletBright,
                activeTrackColor = CosmicVioletBright,
                inactiveTrackColor = CosmicBorder,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Posisi Vertikal: ${(draft.yFraction * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMutedLabel,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Atas" to 0.18f, "Tengah" to 0.50f, "Bawah" to 0.82f).forEach { (lbl, frac) ->
                    Surface(
                        color = if (kotlin.math.abs(draft.yFraction - frac) < 0.04f) CosmicVioletActiveBg else CosmicItemBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CosmicBorder),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onYChange(frac) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = CosmicCreditText)
                        }
                    }
                }
            }
        }
        Slider(
            value = draft.yFraction,
            onValueChange = onYChange,
            valueRange = 0.12f..0.88f,
            colors = SliderDefaults.colors(
                thumbColor = CosmicVioletBright,
                activeTrackColor = CosmicVioletBright,
                inactiveTrackColor = CosmicBorder,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("photo_fx_add_text_button"),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicVioletBright,
                    contentColor = CosmicOnViolet,
                ),
            ) {
                Icon(
                    imageVector = if (draft.editingId != null) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (draft.editingId != null) "Simpan Perubahan Teks" else "Tambahkan Teks ke Animasi",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (draft.editingId != null) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorderHighlight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                ) {
                    Text("Batal")
                }
            }
        }

        if (overlays.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Daftar Teks Aktif (${overlays.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CosmicMutedLabel,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                overlays.forEach { item ->
                    val fontFamily = remember(item.font) { FontFamily(Font(item.font.fontRes)) }
                    Surface(
                        color = CosmicItemBg,
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, CosmicBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    fontFamily = fontFamily,
                                    fontSize = 15.sp,
                                    color = CosmicText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${item.font.label} · ${item.style.label} · ${item.motion.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CosmicMuted,
                                )
                            }
                            IconButton(
                                onClick = { onEditOverlay(item) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit teks",
                                    tint = CosmicCreditText,
                                )
                            }
                            IconButton(
                                onClick = { onRemoveOverlay(item.id) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Hapus teks",
                                    tint = CosmicErrorText,
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private fun formatTimedMs(ms: Long): String {
    val totalTenths = (ms.coerceAtLeast(0L) / 100L)
    val secs = totalTenths / 10L
    val tenths = totalTenths % 10L
    val mins = secs / 60L
    val remSecs = secs % 60L
    return String.format(Locale.US, "%d:%02d.%d", mins, remSecs, tenths)
}
