package com.aitidi.zzztracker.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp,
)

/** System condensed face for counts and version labels, so numbers stay compact without bundling fonts. */
val NumberFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

fun numberStyle(size: Int) = TextStyle(fontFamily = NumberFamily, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.15f).sp)

val AppTypography = Typography(
    headlineLarge = type(28, 36, FontWeight.Bold),
    headlineMedium = type(24, 32, FontWeight.Bold),
    titleLarge = type(20, 28, FontWeight.Bold),
    titleMedium = type(16, 23, FontWeight.Medium),
    titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 21),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.Medium),
    labelMedium = type(12, 17, FontWeight.Medium),
    labelSmall = type(11, 15, FontWeight.Medium),
)
