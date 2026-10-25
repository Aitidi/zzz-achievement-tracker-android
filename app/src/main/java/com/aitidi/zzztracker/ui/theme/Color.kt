package com.aitidi.zzztracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** One palette per theme; the amber accent stays the same so completion reads identically in both. */
@Immutable
data class AppPalette(
    val background: Color,
    val surface: Color,
    val control: Color,
    val text: Color,
    val secondary: Color,
    val separator: Color,
    val accent: Color,
    val accentDeep: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightPalette = AppPalette(
    background = Color(0xFFECEDF0),
    surface = Color(0xFFFFFFFF),
    control = Color(0xFFF4F5F7),
    text = Color(0xFF16171B),
    secondary = Color(0xFF686B74),
    separator = Color(0xFFDFE1E6),
    accent = Color(0xFFFFC21A),
    accentDeep = Color(0xFFE8A900),
    onAccent = Color(0xFF16171B),
    accentText = Color(0xFF8A5F00),
    accentSoft = Color(0xFFFFF4D1),
    danger = Color(0xFFC8372A),
    isDark = false,
)

val DarkPalette = AppPalette(
    background = Color(0xFF0D0E11),
    surface = Color(0xFF18191E),
    control = Color(0xFF212228),
    text = Color(0xFFF0F1F3),
    secondary = Color(0xFF9A9DA7),
    separator = Color(0xFF2B2D34),
    accent = Color(0xFFFFC21A),
    accentDeep = Color(0xFFD99D00),
    onAccent = Color(0xFF16171B),
    accentText = Color(0xFFFFCB45),
    accentSoft = Color(0xFF2E2610),
    danger = Color(0xFFFF6B5E),
    isDark = true,
)

val LocalAppPalette = staticCompositionLocalOf { LightPalette }

/** Theme-aware shortcuts; read them in composition and capture before drawing. */
object AppColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.surface
    val Control: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.control
    val Text: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.text
    val Secondary: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.secondary
    val Separator: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.separator
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.accent
    val AccentDeep: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.accentDeep
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.onAccent
    val AccentText: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.accentText
    val AccentSoft: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.accentSoft
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.danger
}
