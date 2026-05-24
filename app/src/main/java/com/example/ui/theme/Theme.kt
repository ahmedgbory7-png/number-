package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HighDensityColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    secondary = HighDensityContainer,
    onSecondary = HighDensityOnContainer,
    background = HighDensityBg,
    onBackground = HighDensityOnSurface,
    surface = HighDensitySurface,
    onSurface = HighDensityOnSurface,
    surfaceVariant = HighDensityBorder,
    onSurfaceVariant = HighDensityMutedText,
    error = ErrorOrange
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set to false to match the High Density clean light styling
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}
