package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ApiKeyBalance
import com.example.model.CreditCalculator
import com.example.ui.components.CosmicCard
import com.example.ui.components.cosmicInputColors
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCardTop
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicNoticeBg
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicPillBg
import com.example.ui.theme.CosmicPillBorder
import com.example.ui.theme.CosmicSuccess
import com.example.ui.theme.CosmicSurfaceAlt
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicVioletActiveBg
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.VideoDownloader
import com.example.viewmodel.KangbasUiState

@Composable
fun SettingsScreen(
    state: KangbasUiState,
    userEmail: String,
    userDisplayName: String,
    onSignOut: () -> Unit,
    onKeyInputChange: (String) -> Unit,
    onSaveKeys: () -> Unit,
    onToggleShowKeys: () -> Unit,
    onCheckBalance: () -> Unit,
    onSelectActiveKey: (Int) -> Unit,
    onRemoveKeyAt: (Int) -> Unit,
    onCopyAllKeys: () -> Unit,
    onRequestDeleteAllKeys: () -> Unit,
    onDismissDeleteAllKeys: () -> Unit,
    onConfirmDeleteAllKeys: () -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        // Gmail Account & Persistent Memory Card
        CosmicCard(borderColor = CosmicBorderHighlight.copy(alpha = 0.55f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = CosmicVioletBright,
                    modifier = Modifier.size(34.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.account_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmicText,
                    )
                    Text(
                        text = userDisplayName.ifBlank { "Pengguna Kangbas" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = CosmicText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = CosmicPillBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CosmicPillBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = CosmicSuccess,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.account_sync_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicCreditText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicCreditText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // API Key Configuration Card
        CosmicCard {
            Text(
                text = stringResource(R.string.settings_api_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_api_desc),
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Summed Credit Banner (reflects user's explicit request to sum credits from all API keys)
            Surface(
                color = CosmicNoticeBg,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, CosmicBorderHighlight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_credit_sum_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicCreditText,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_credit_sum_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicCreditText.copy(alpha = 0.88f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_steps),
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { VideoDownloader.openWebUrl(context, "https://kie.ai/api-key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_kie_portal_button"),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, CosmicBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CosmicSurfaceAlt,
                    contentColor = CosmicCreditText,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_open_kie_portal),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_api_key),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CosmicMutedLabel,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = state.keyInput,
                onValueChange = onKeyInputChange,
                placeholder = { Text(text = stringResource(R.string.api_key_placeholder)) },
                minLines = 4,
                maxLines = 6,
                visualTransformation = if (state.showKeys) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                colors = cosmicInputColors(),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSaveKeys,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("save_key_button"),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicVioletBright,
                        contentColor = CosmicOnViolet,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.action_save_key),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                OutlinedButton(
                    onClick = onToggleShowKeys,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("toggle_show_key_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.showKeys) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.showKeys) {
                            stringResource(R.string.action_hide)
                        } else {
                            stringResource(R.string.action_show)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCheckBalance,
                    enabled = !state.isRefreshingCredit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("check_balance_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.action_check_balance),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                OutlinedButton(
                    onClick = onRequestDeleteAllKeys,
                    enabled = state.keyPool.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("delete_keys_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicErrorText,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.action_delete_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.keyStatusText,
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
                modifier = Modifier.testTag("key_status_text"),
            )
        }

        // Saved Keys List Card
        CosmicCard {
            Text(
                text = stringResource(R.string.saved_keys_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val totalLabel = state.totalCredits?.let { CreditCalculator.formatIdNumber(it) } ?: "—"
            val hasNegative = state.keyBalances.any { (it.balance ?: 0.0) < 0.0 }
            Text(
                text = if (hasNegative) {
                    "Jumlah kunci: ${state.keyPool.size} · Total kredit gabungan: $totalLabel (saldo minus dihitung 0)"
                } else {
                    "Jumlah kunci: ${state.keyPool.size} · Total kredit gabungan: $totalLabel"
                },
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onToggleShowKeys,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("show_all_keys_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = if (state.showKeys) {
                            stringResource(R.string.action_hide)
                        } else {
                            stringResource(R.string.action_show)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }

                OutlinedButton(
                    onClick = onCopyAllKeys,
                    enabled = state.keyPool.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("copy_all_keys_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_copy_all),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }

                OutlinedButton(
                    onClick = onRequestDeleteAllKeys,
                    enabled = state.keyPool.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("delete_all_keys_button"),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicErrorText,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete_all),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (state.keyPool.isEmpty()) {
                Text(
                    text = stringResource(R.string.saved_keys_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                )
            } else {
                val balanceByKey = state.keyBalances.associateBy { it.key }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.keyPool.forEachIndexed { index, key ->
                        SavedKeyCard(
                            index = index,
                            keyValue = key,
                            showFull = state.showKeys,
                            isActive = index == state.activeKeyIndex,
                            balanceInfo = balanceByKey[key],
                            onUseClick = { onSelectActiveKey(index) },
                            onDeleteClick = { onRemoveKeyAt(index) },
                        )
                    }
                }
            }
        }

        // About Card
        CosmicCard {
            Text(
                text = stringResource(R.string.about_heading),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicText,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.about_desc),
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

    if (state.showDeleteKeysConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDeleteAllKeys,
            containerColor = CosmicCardTop,
            titleContentColor = CosmicText,
            textContentColor = CosmicMuted,
            title = { Text(text = stringResource(R.string.confirm_delete_keys_title)) },
            text = { Text(text = stringResource(R.string.confirm_delete_keys_desc)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDeleteAllKeys,
                    modifier = Modifier.testTag("confirm_delete_keys_button"),
                ) {
                    Text(text = stringResource(R.string.action_delete), color = CosmicErrorText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteAllKeys) {
                    Text(text = stringResource(R.string.action_cancel), color = CosmicCreditText)
                }
            },
        )
    }
}

@Composable
private fun SavedKeyCard(
    index: Int,
    keyValue: String,
    showFull: Boolean,
    isActive: Boolean,
    balanceInfo: ApiKeyBalance?,
    onUseClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val displayedKey = if (showFull) {
        keyValue
    } else if (keyValue.length > 10) {
        "${keyValue.take(5)}••••••••••${keyValue.takeLast(4)}"
    } else {
        "••••••••••"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_key_card_$index"),
        color = CosmicItemBg,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (isActive) CosmicBorderHighlight else CosmicBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kunci ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = CosmicText,
                )
                val bal = balanceInfo?.balance
                val balText = when {
                    bal != null && bal < 0.0 -> "${CreditCalculator.formatIdNumber(bal)} kredit (dihitung 0)"
                    bal != null -> "${CreditCalculator.formatIdNumber(bal)} kredit"
                    !balanceInfo?.error.isNullOrBlank() -> "Gagal cek"
                    else -> "Belum dicek"
                }
                val balColor = when {
                    bal != null && bal > 0.0 -> CosmicSuccess
                    bal != null && bal < 0.0 -> CosmicErrorText
                    bal != null -> CosmicMuted
                    !balanceInfo?.error.isNullOrBlank() -> CosmicErrorText
                    else -> CosmicMuted
                }
                Text(
                    text = balText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = balColor,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayedKey,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = CosmicMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!balanceInfo?.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = balanceInfo?.error.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicErrorText,
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onUseClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("use_key_button_$index"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isActive) CosmicBorderHighlight else CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isActive) CosmicVioletActiveBg else CosmicSurfaceAlt,
                        contentColor = CosmicText,
                    ),
                ) {
                    Text(
                        text = if (isActive) {
                            stringResource(R.string.status_active)
                        } else {
                            stringResource(R.string.action_use)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("delete_key_button_$index"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CosmicSurfaceAlt,
                        contentColor = CosmicErrorText,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
