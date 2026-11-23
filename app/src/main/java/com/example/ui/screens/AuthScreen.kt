package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CosmicCard
import com.example.ui.theme.CoralHeaderEnd
import com.example.ui.theme.CoralHeaderStart
import com.example.ui.theme.CosmicBg
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicCreditText
import com.example.ui.theme.CosmicErrorBg
import com.example.ui.theme.CosmicErrorText
import com.example.ui.theme.CosmicGlow
import com.example.ui.theme.CosmicItemBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicOnViolet
import com.example.ui.theme.CosmicPillBg
import com.example.ui.theme.CosmicPillBorder
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicVioletBright
import com.example.ui.theme.CosmicVioletDeep

@Composable
fun AuthScreen(
    isSigningIn: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBg)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicGlow.copy(alpha = 0.55f), Color.Transparent),
                    startY = 0f,
                    endY = 760f,
                )
            ),
    ) {
        // Radiant Coral Red Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(CoralHeaderStart, CoralHeaderEnd)))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 520.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE50914))
                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.90f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.widthIn(max = 520.dp)) {
                CosmicCard(borderColor = CosmicBorderHighlight.copy(alpha = 0.55f)) {
                    Surface(
                        color = CosmicPillBg,
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, CosmicPillBorder),
                    ) {
                        Text(
                            text = "MEMORI MULTI-AKUN GMAIL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = CosmicCreditText,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.auth_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.auth_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted,
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    AuthFeatureRow(
                        icon = Icons.Rounded.CloudDone,
                        title = "Riwayat Video Tersimpan Otomatis",
                        subtitle = "Semua histori Text to Video, Animasi Foto, dan Teks Otomatis tersimpan aman di akun Anda.",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AuthFeatureRow(
                        icon = Icons.Rounded.SwitchAccount,
                        title = "Aman Meski Ganti Akun Gmail",
                        subtitle = "Keluar ke akun Gmail lain lalu kembali lagi ke akun ini — histori lama tidak berubah atau hilang.",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AuthFeatureRow(
                        icon = Icons.Rounded.History,
                        title = "Siap Edit Ulang Kapan Saja",
                        subtitle = "Draft animasi & teks otomatis Anda tetap terpisah rapi per akun Gmail.",
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = CosmicErrorBg,
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, CosmicErrorText.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicErrorText,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .testTag("auth_error_text"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val ctaBrush = Brush.linearGradient(listOf(CosmicVioletBright, CosmicVioletDeep))
                    Button(
                        onClick = onGoogleSignInClick,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .background(
                                brush = ctaBrush,
                                shape = RoundedCornerShape(12.dp),
                                alpha = if (isSigningIn) 0.65f else 1f,
                            )
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = CosmicOnViolet,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = CosmicOnViolet,
                        ),
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.4.dp,
                                color = CosmicOnViolet,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auth_signing_in),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_google_sign_in),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Surface(
        color = CosmicItemBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CosmicBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicVioletBright,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = CosmicText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                )
            }
        }
    }
}
