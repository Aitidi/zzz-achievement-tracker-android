package com.aitidi.zzztracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColors = darkColorScheme(
    primary = ZzzPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF32295A),
    onPrimaryContainer = ZzzPurpleSoft,

    secondary = ZzzAccent,
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF2F3140),
    onSecondaryContainer = ZzzText,

    tertiary = ZzzPurpleSoft,
    onTertiary = Color(0xFF111111),
    tertiaryContainer = Color(0xFF2F3140),
    onTertiaryContainer = ZzzText,

    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,

    onBackground = ZzzText,
    onSurface = ZzzText,
    outline = ZzzBorder,
)

private val LightColors = lightColorScheme(
    primary = ZzzPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3DCFF),
    onPrimaryContainer = Color(0xFF2F225F),

    secondary = Color(0xFF637900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F8C8),
    onSecondaryContainer = Color(0xFF1E2A00),

    tertiary = ZzzPurpleSoft,
    onTertiary = Color(0xFF2E255A),
    tertiaryContainer = Color(0xFFEDE7FF),
    onTertiaryContainer = Color(0xFF2E255A),

    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,

    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    outline = Color(0xFFD2C9F8),
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
