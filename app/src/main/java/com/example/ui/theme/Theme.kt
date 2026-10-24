package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBlack,
    primaryContainer = DeepViolet,
    onPrimaryContainer = TextCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = CyberSurfaceElevated,
    onSecondaryContainer = TextWhite,
    tertiary = CyberGreen,
    onTertiary = CyberBlack,
    background = CyberBackground,
    onBackground = TextWhite,
    surface = CyberSurface,
    onSurface = TextWhite,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = CyberSurfaceBorder,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
