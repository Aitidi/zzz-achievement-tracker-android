package com.aitidi.zzztracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun ZzzTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
