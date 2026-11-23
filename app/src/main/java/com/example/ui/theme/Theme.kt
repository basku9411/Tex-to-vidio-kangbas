package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KangbasColorScheme = darkColorScheme(
    primary = CosmicViolet,
    onPrimary = CosmicOnViolet,
    primaryContainer = CosmicVioletActiveBg,
    onPrimaryContainer = CosmicVioletActiveText,
    secondary = CosmicVioletBright,
    onSecondary = CosmicOnViolet,
    secondaryContainer = CosmicSurfaceAlt,
    onSecondaryContainer = CosmicText,
    tertiary = CosmicGold,
    onTertiary = CosmicBg,
    background = CosmicBg,
    onBackground = CosmicText,
    surface = CosmicSurface,
    onSurface = CosmicText,
    surfaceVariant = CosmicSurfaceAlt,
    onSurfaceVariant = CosmicMuted,
    surfaceContainerLowest = CosmicInputBg,
    surfaceContainerLow = CosmicItemBg,
    surfaceContainer = CosmicSurface,
    surfaceContainerHigh = CosmicCardTop,
    surfaceContainerHighest = CosmicSurfaceAlt,
    outline = CosmicBorder,
    outlineVariant = CosmicBorder,
    error = CosmicError,
    onError = CosmicOnViolet,
    errorContainer = CosmicErrorBg,
    onErrorContainer = CosmicErrorText,
)

private val KangbasShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun KangbasTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KangbasColorScheme,
        typography = KangbasTypography,
        shapes = KangbasShapes,
        content = content,
    )
}
