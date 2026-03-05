package com.aitidi.zzztracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColors = darkColorScheme(
    primary = IOSBlue,
    secondary = IOSGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
)

private val LightColors = lightColorScheme(
    primary = IOSBlue,
    secondary = IOSGreen,
    background = LightBackground,
    surface = LightSurface,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
)

@Composable
fun ZzzTrackerTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
