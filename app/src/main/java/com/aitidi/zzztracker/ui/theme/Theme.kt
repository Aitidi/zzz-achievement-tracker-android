package com.aitidi.zzztracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AppColors.Blue,
    onPrimary = Color.White,
    primaryContainer = AppColors.BlueSoft,
    onPrimaryContainer = AppColors.Blue,
    secondary = AppColors.Blue,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.Control,
    onSurfaceVariant = AppColors.Secondary,
    outline = AppColors.Secondary,
    outlineVariant = AppColors.Separator,
    error = AppColors.Red,
    surfaceTint = Color.Transparent,
)

@Composable
fun ZzzTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)
}
