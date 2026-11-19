package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MidnightSlateColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041E2B),
    primaryContainer = ElectricCyanDim,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = StudioAmber,
    onSecondary = Color(0xFF281800),
    secondaryContainer = StudioAmberDim,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = MintReady,
    onTertiary = Color(0xFF042419),
    tertiaryContainer = MintReadyDim,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = MidnightBg,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateElevated,
    surfaceContainerHigh = SlateHighest,
    surfaceContainerHighest = Color(0xFF2A3548),
    outline = SlateOutline,
    outlineVariant = Color(0xFF1F2837),
    error = CrimsonError,
    onError = Color(0xFF2E0711),
    errorContainer = CrimsonErrorDim,
    onErrorContainer = Color(0xFFFECDD3)
)

val SlateShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SlateCutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MidnightSlateColorScheme,
        typography = Typography,
        shapes = SlateShapes,
        content = content
    )
}
