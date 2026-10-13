package com.vela.notes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vela.notes.R

private val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
)

private val AndroidSans = FontFamily.SansSerif

internal val VelaTypography = Typography(
    displayLarge = velaTextStyle(Inter, FontWeight.Bold, 40, 46),
    displayMedium = velaTextStyle(Inter, FontWeight.Bold, 36, 42),
    displaySmall = velaTextStyle(Inter, FontWeight.Bold, 34, 40),
    headlineLarge = velaTextStyle(Inter, FontWeight.Bold, 30, 36),
    headlineMedium = velaTextStyle(Inter, FontWeight.SemiBold, 26, 32),
    headlineSmall = velaTextStyle(Inter, FontWeight.SemiBold, 22, 28),
    titleLarge = velaTextStyle(Inter, FontWeight.SemiBold, 20, 26),
    titleMedium = velaTextStyle(AndroidSans, FontWeight.SemiBold, 17, 22),
    titleSmall = velaTextStyle(AndroidSans, FontWeight.Medium, 15, 20),
    bodyLarge = velaTextStyle(AndroidSans, FontWeight.Normal, 17, 24),
    bodyMedium = velaTextStyle(AndroidSans, FontWeight.Normal, 15, 21),
    bodySmall = velaTextStyle(AndroidSans, FontWeight.Normal, 13, 18),
    labelLarge = velaTextStyle(AndroidSans, FontWeight.SemiBold, 14, 20),
    labelMedium = velaTextStyle(AndroidSans, FontWeight.Medium, 12, 16),
    labelSmall = velaTextStyle(AndroidSans, FontWeight.Medium, 11, 14),
)

private fun velaTextStyle(
    family: FontFamily,
    weight: FontWeight,
    sizeSp: Int,
    lineHeightSp: Int,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = 0.sp,
)
