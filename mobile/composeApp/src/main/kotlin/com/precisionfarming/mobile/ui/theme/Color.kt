package com.precisionfarming.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/** AgOS "Terra" tokens (mirror of agos.css). Screens must use these, never literal hex. */
object AgOsColors {
    // Brand green
    val g950 = Color(0xFF0B2318)
    val g900 = Color(0xFF0F2E1F)
    val g800 = Color(0xFF16402C)
    val g700 = Color(0xFF1F5A3C)
    val g600 = Color(0xFF2A7350)
    val g500 = Color(0xFF3A8F66)
    val g100 = Color(0xFFE3F0E8)
    val g50 = Color(0xFFF0F7F2)

    // Action teal
    val t700 = Color(0xFF0B6E62)
    val t600 = Color(0xFF0E8A7A)
    val t500 = Color(0xFF12A08E)
    val t100 = Color(0xFFD6F1EC)
    val t50 = Color(0xFFEBF8F5)

    // Warm neutrals
    val n0 = Color(0xFFFFFFFF)
    val n50 = Color(0xFFF7F6F1)
    val n100 = Color(0xFFEFEDE6)
    val n200 = Color(0xFFE2DFD5)
    val n300 = Color(0xFFCFCBBE)
    val n400 = Color(0xFFAAA697)
    val n500 = Color(0xFF8A877B)
    val n600 = Color(0xFF6A675D)
    val n700 = Color(0xFF4E4C45)
    val n800 = Color(0xFF33322D)
    val n900 = Color(0xFF1E1D1A)

    // Status
    val crit = Color(0xFFB42318)
    val critBg = Color(0xFFFEE4E2)
    val warn = Color(0xFFB54708)
    val warnBg = Color(0xFFFEF0C7)
    val warnBorder = Color(0xFFF2C48F)
    val ok = Color(0xFF1F7A4C)
    val okBg = Color(0xFFDCFAE6)
    val info = Color(0xFF175CD3)
    val infoBg = Color(0xFFE0EAFF)

    // Confidence ramp
    val confidenceHigh = Color(0xFF0E8A7A)
    val confidenceMedium = Color(0xFFB7791F)
    val confidenceLow = Color(0xFFC2410C)

    // Field states
    val fieldPlanned = Color(0xFFD9C9A3)
    val fieldProgress = Color(0xFF12A08E)
    val fieldDone = Color(0xFF2A7350)
    val fieldBlocked = Color(0xFFB42318)
    val fieldStale = Color(0xFFAAA697)

    // Text on dark hero / login surfaces
    val heroText = Color(0xFFEAF2EC)
    val heroMuted = Color(0xFFA8C4B3)
    val heroChip = Color(0x24FFFFFF)
    val heroOutline = Color(0x40FFFFFF)
    val onDarkStrip = Color(0xFFE8E6DF)
}
