package com.aitidi.zzztracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColors = darkColorScheme(
    primary = IOSBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0x332A9BFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),

    secondary = IOSBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0x332A9BFF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),

    tertiary = IOSBlue,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0x332A9BFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),

    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurface,

    onBackground = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
)

private val LightColors = lightColorScheme(
    primary = IOSBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF004A87),

    secondary = IOSBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF004A87),

    tertiary = IOSBlue,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFD9ECFF),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF004A87),

    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightBackground,

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
