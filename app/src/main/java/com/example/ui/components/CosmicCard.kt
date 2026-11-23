package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicCardBottom
import com.example.ui.theme.CosmicCardTop
import com.example.ui.theme.CosmicInputBg
import com.example.ui.theme.CosmicMuted
import com.example.ui.theme.CosmicMutedLabel
import com.example.ui.theme.CosmicText
import com.example.ui.theme.CosmicViolet

@Composable
fun CosmicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CosmicBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(listOf(CosmicCardTop, CosmicCardBottom)),
                shape = shape,
            )
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun cosmicInputColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CosmicInputBg,
    unfocusedContainerColor = CosmicInputBg,
    disabledContainerColor = CosmicInputBg,
    focusedBorderColor = CosmicViolet,
    unfocusedBorderColor = CosmicBorder,
    disabledBorderColor = CosmicBorder.copy(alpha = 0.5f),
    focusedTextColor = CosmicText,
    unfocusedTextColor = CosmicText,
    disabledTextColor = CosmicMuted,
    focusedLabelColor = CosmicMutedLabel,
    unfocusedLabelColor = CosmicMutedLabel,
    focusedPlaceholderColor = CosmicMuted.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = CosmicMuted.copy(alpha = 0.6f),
    cursorColor = CosmicViolet,
)
