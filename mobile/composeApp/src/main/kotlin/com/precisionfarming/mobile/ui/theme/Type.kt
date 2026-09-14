package com.precisionfarming.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** System fonts only — no font download on mobile. Display = heavy sans, mono for IDs/timers. */
val AgOsMono: FontFamily = FontFamily.Monospace

private val display = FontFamily.SansSerif

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    family: FontFamily = FontFamily.Default,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    lineHeightStyle = lineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val AgOsTypography = Typography(
    displayLarge = style(34, FontWeight.ExtraBold, 40, display, -0.02f),
    displayMedium = style(28, FontWeight.ExtraBold, 34, display, -0.02f),
    displaySmall = style(24, FontWeight.ExtraBold, 30, display, -0.02f),
    headlineLarge = style(24, FontWeight.Bold, 30, display, -0.01f),
    headlineMedium = style(22, FontWeight.Bold, 28, display, -0.01f),
    headlineSmall = style(19, FontWeight.Bold, 24, display, -0.01f),
    titleLarge = style(19, FontWeight.Bold, 24, display, -0.01f),
    titleMedium = style(15, FontWeight.SemiBold, 20),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(15, FontWeight.Normal, 22),
    bodyMedium = style(13, FontWeight.Normal, 18),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(13, FontWeight.SemiBold, 18),
    labelMedium = style(11, FontWeight.SemiBold, 16, letterSpacing = 0.04f),
    labelSmall = style(11, FontWeight.Medium, 16),
)

/** Mono style for ids, timestamps and timers. */
val MonoSmall = TextStyle(
    fontFamily = AgOsMono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val MonoTimer = TextStyle(
    fontFamily = AgOsMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.02f).em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
