package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.NoticeMessage
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicNoticeBg
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicVioletDeep

@Composable
fun NoticeDialog(
    notice: NoticeMessage,
    onDismiss: () -> Unit,
    onOpenQris: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 390.dp),
            shape = RoundedCornerShape(22.dp),
            color = CosmicNoticeBg,
            border = BorderStroke(1.dp, CosmicBorderHighlight),
            tonalElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(23.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = notice.icon,
                    fontSize = 36.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = notice.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicCreditText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("notice_close_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CosmicBorderHighlight.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = stringResource(R.string.action_later),
                            color = CosmicCreditText,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Button(
                        onClick = onOpenQris,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("notice_qris_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicVioletDeep,
                            contentColor = CosmicOnViolet,
                        ),
                    ) {
                        Text(
                            text = "☕ ${stringResource(R.string.action_qris)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
