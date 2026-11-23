package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AiModel
import com.example.model.AspectRatioOption
import com.example.model.CreditCalculator
import com.example.model.DurationOption
import com.example.model.GenerationMode
import com.example.model.ModelGroup
import com.example.model.ResolutionOption
import com.example.ui.components.CosmicCard
import com.example.ui.components.cosmicInputColors
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicInputBg
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicSuccess
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletActiveText
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.CosmicVioletDeep
import com.example.viewmodel.KangbasUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    state: KangbasUiState,
    onModeChange: (GenerationMode) -> Unit,
    onModelSelect: (AiModel) -> Unit,
    onPromptChange: (String) -> Unit,
    onRatioSelect: (AspectRatioOption) -> Unit,
    onResolutionSelect: (ResolutionOption) -> Unit,
    onDurationSelect: (DurationOption) -> Unit,
    onAudioToggle: (Boolean) -> Unit,
    onImagePicked: (android.net.Uri?) -> Unit,
    onClearImage: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onImagePicked,
    )

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        CosmicCard {
            Text(
                text = stringResource(R.string.create_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModeButton(
                    label = stringResource(R.string.mode_text_to_video),
                    icon = Icons.Rounded.EditNote,
                    selected = state.mode == GenerationMode.TEXT,
                    onClick = { onModeChange(GenerationMode.TEXT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_mode_button"),
                )
                ModeButton(
                    label = stringResource(R.string.mode_image_to_video),
                    icon = Icons.Rounded.Image,
                    selected = state.mode == GenerationMode.IMAGE,
                    onClick = { onModeChange(GenerationMode.IMAGE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("image_mode_button"),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionLabel(text = stringResource(R.string.label_select_model))

            // Model dropdown with grouped headers
            var modelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded },
            ) {
                OutlinedTextField(
                    value = state.selectedModel.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    colors = cosmicInputColors(),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("model_dropdown"),
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false },
                    modifier = Modifier.background(CosmicSurface),
                ) {
                    ModelGroup.entries.forEachIndexed { groupIdx, group ->
                        if (groupIdx > 0) {
                            HorizontalDivider(color = CosmicBorder)
                        }
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CosmicVioletBright,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        AiModel.entries.filter { it.group == group }.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = model.label,
                                        color = if (model == state.selectedModel) CosmicVioletBright else CosmicText,
                                        fontWeight = if (model == state.selectedModel) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onModelSelect(model)
                                    modelExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.selectedModel.hint,
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
            )

            Spacer(modifier = Modifier.height(10.dp))
            // Price Estimate Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("price_estimate_card"),
                color = CosmicItemBg,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    val credits = state.priceEstimate.credits
                    Text(
                        text = if (credits == null) {
                            stringResource(R.string.price_estimate_unknown)
                        } else {
                            stringResource(R.string.price_estimate_format, CreditCalculator.formatIdNumber(credits))
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicCreditText,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state.priceEstimate.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicMuted,
                    )
                }
            }

            // Image Picker Area
            AnimatedVisibility(visible = state.mode == GenerationMode.IMAGE) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionLabel(text = stringResource(R.string.label_choose_photo))

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pick_photo_button"),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, CosmicBorderHighlight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CosmicSurfaceAlt,
                            contentColor = CosmicText,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.selectedImage == null) {
                                stringResource(R.string.action_pick_photo)
                            } else {
                                stringResource(R.string.action_change_photo)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    state.selectedImage?.let { img ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicInputBg)
                                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp)),
                        ) {
                            AsyncImage(
                                model = img.uri,
                                contentDescription = stringResource(R.string.cd_selected_photo_preview),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .padding(4.dp),
                            )
                            IconButton(
                                onClick = onClearImage,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(48.dp)
                                    .testTag("clear_photo_button"),
                            ) {
                                Surface(
                                    color = CosmicSurface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.action_clear_photo),
                                        tint = CosmicText,
                                        modifier = Modifier.padding(6.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val sizeMb = img.sizeBytes / (1024.0 * 1024.0)
                        Text(
                            text = "${img.fileName} (${String.format("%.2f", sizeMb)} MB)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicCreditText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.photo_upload_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionLabel(text = stringResource(R.string.label_prompt))
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.prompt_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                minLines = 4,
                maxLines = 7,
                colors = cosmicInputColors(),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_input"),
            )
            Text(
                text = "${state.prompt.length} / 5000",
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Ratio selector
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(text = stringResource(R.string.label_ratio))
                    SimpleDropdownField(
                        selectedLabel = state.ratio.label,
                        items = AspectRatioOption.entries,
                        itemLabel = { it.label },
                        itemEnabled = { true },
                        onSelect = onRatioSelect,
                        testTag = "ratio_dropdown",
                    )
                }
                // Resolution selector
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(text = stringResource(R.string.label_resolution))
                    SimpleDropdownField(
                        selectedLabel = state.resolution.label,
                        items = ResolutionOption.entries,
                        itemLabel = { it.label },
                        itemEnabled = { it != ResolutionOption.R480 || state.selectedModel.supports480p },
                        onSelect = onResolutionSelect,
                        testTag = "resolution_dropdown",
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel(text = stringResource(R.string.label_duration))
            SimpleDropdownField(
                selectedLabel = state.duration.label,
                items = DurationOption.entries,
                itemLabel = { it.label },
                itemEnabled = { true },
                onSelect = onDurationSelect,
                testTag = "duration_dropdown",
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAudioToggle(!state.audioEnabled) }
                    .padding(vertical = 4.dp)
                    .testTag("audio_checkbox_row"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.audioEnabled,
                    onCheckedChange = onAudioToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = CosmicViolet,
                        uncheckedColor = CosmicMuted,
                        checkmarkColor = CosmicOnViolet,
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_audio),
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Generate CTA
            val gradientBrush = Brush.linearGradient(
                listOf(CosmicVioletBright, CosmicVioletDeep)
            )
            Button(
                onClick = onGenerateClick,
                enabled = !state.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(
                        brush = gradientBrush,
                        shape = RoundedCornerShape(12.dp),
                        alpha = if (state.isBusy) 0.55f else 1f,
                    )
                    .testTag("generate_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CosmicOnViolet,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = CosmicOnViolet,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.4.dp,
                        color = CosmicOnViolet,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_generating),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_generate),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.statusIsError) CosmicErrorText else CosmicSuccess,
                modifier = Modifier.testTag("create_status_text"),
            )
        }

        // How-to Card
        CosmicCard {
            Text(
                text = stringResource(R.string.howto_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.howto_body),
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )
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
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) CosmicVioletActiveText else CosmicMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = CosmicMutedLabel,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SimpleDropdownField(
    selectedLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    itemEnabled: (T) -> Boolean,
    onSelect: (T) -> Unit,
    testTag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = cosmicInputColors(),
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(testTag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CosmicSurface),
        ) {
            items.forEach { item ->
                val enabled = itemEnabled(item)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            color = if (enabled) CosmicText else CosmicMuted.copy(alpha = 0.45f),
                        )
                    },
                    enabled = enabled,
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
