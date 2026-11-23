package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CreditCalculator
import com.example.ui.theme.CoralHeaderEnd
import com.example.ui.theme.CoralHeaderStart
import com.example.ui.theme.CosmicBorderHighlight
import com.example.ui.theme.CosmicVioletDeep

@Composable
fun TopHeader(
    totalCredits: Double?,
    keyCount: Int,
    validKeyCount: Int,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onQrisClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(CoralHeaderStart, CoralHeaderEnd)))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = Color(0xFFE50914),
                            shape = RoundedCornerShape(11.dp),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                val creditText = if (totalCredits != null) {
                    stringResource(R.string.credit_format, CreditCalculator.formatIdNumber(totalCredits))
                } else {
                    stringResource(R.string.credit_unknown)
                }

                Surface(
                    modifier = Modifier.testTag("credit_badge"),
                    color = Color.White,
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, Color(0xFFE50914)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = creditText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                            ),
                            color = Color(0xFF0A0A0C),
                            maxLines = 1,
                        )
                        if (keyCount > 1 && totalCredits != null) {
                            Text(
                                text = "Total $validKeyCount/$keyCount kunci",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = CosmicVioletDeep,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .testTag("refresh_credit_button")
                            .defaultMinSize(minHeight = 40.dp),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1E1E24),
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.credit_checking),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                                modifier = Modifier.size(14.dp),
                                tint = Color.White,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_refresh),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onQrisClick,
                        modifier = Modifier
                            .testTag("open_qris_button")
                            .defaultMinSize(minHeight = 40.dp),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, Color(0xFFE50914)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF2B080C),
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Coffee,
                            contentDescription = stringResource(R.string.action_qris),
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFF4D5A),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_qris),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = CosmicBorderHighlight.copy(alpha = 0.75f), thickness = 1.dp)
    }
}
