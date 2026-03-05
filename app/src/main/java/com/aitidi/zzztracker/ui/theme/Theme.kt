package com.aitidi.zzztracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = NeonBlue,
    secondary = Mint,
    tertiary = Violet,
    background = DarkBg,
    surface = DarkSurface,
)

private val LightColors = lightColorScheme(
    primary = NeonBlue,
    secondary = Mint,
    tertiary = Violet,
    background = LightBg,
    surface = LightSurface,
)

@Composable
fun ZzzTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
