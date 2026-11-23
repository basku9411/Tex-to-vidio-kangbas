package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCardTop
import com.example.ui.theme.CosmicGold
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicVioletBright

@Composable
fun QrisDialog(
    remainingSeconds: Int,
    onContinue: () -> Unit,
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 390.dp),
            shape = RoundedCornerShape(20.dp),
            color = CosmicCardTop,
            border = BorderStroke(1.dp, CosmicBorderHighlight.copy(alpha = 0.65f)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.qris_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = CosmicText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.qris_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(0.86f),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qris_shopeepay),
                        contentDescription = stringResource(R.string.cd_qris_image),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Terima kasih atas dukungannya. Kamu bisa langsung klik lanjutkan kapan saja.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = CosmicGold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onContinue,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .alpha(1f)
                        .testTag("qris_continue_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicVioletBright,
                        contentColor = CosmicOnViolet,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.action_continue),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
