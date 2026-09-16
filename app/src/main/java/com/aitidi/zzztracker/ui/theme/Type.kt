package com.aitidi.zzztracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp,
)

val AppTypography = Typography(
    headlineLarge = type(34, 42, FontWeight.Bold),
    headlineMedium = type(28, 36, FontWeight.Bold),
    titleLarge = type(22, 30, FontWeight.SemiBold),
    titleMedium = type(17, 25, FontWeight.SemiBold),
    titleSmall = type(15, 22, FontWeight.SemiBold),
    bodyLarge = type(17, 25),
    bodyMedium = type(15, 22),
    bodySmall = type(13, 19),
    labelLarge = type(15, 22, FontWeight.Medium),
    labelMedium = type(13, 19, FontWeight.Medium),
    labelSmall = type(11, 16, FontWeight.Medium),
)
