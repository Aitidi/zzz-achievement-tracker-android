package com.aitidi.zzztracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.aitidi.zzztracker.viewmodel.ThemeMode

private fun AppPalette.colorScheme() = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accentSoft,
    onPrimaryContainer = accentText,
    secondary = accentText,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = control,
    onSurfaceVariant = secondary,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerLow = background,
    outline = secondary,
    outlineVariant = separator,
    inverseSurface = text,
    inverseOnSurface = surface,
    inversePrimary = accent,
    error = danger,
    surfaceTint = Color.Transparent,
)

@Composable
fun ZzzTrackerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = palette.colorScheme(), typography = AppTypography, content = content)
    }
}
