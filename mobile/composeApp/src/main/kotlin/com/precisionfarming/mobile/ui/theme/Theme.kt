package com.precisionfarming.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TerraLight = lightColorScheme(
    primary = AgOsColors.t600,
    onPrimary = AgOsColors.n0,
    primaryContainer = AgOsColors.t100,
    onPrimaryContainer = AgOsColors.t700,
    inversePrimary = AgOsColors.t100,
    secondary = AgOsColors.g700,
    onSecondary = AgOsColors.n0,
    secondaryContainer = AgOsColors.g100,
    onSecondaryContainer = AgOsColors.g900,
    tertiary = AgOsColors.warn,
    onTertiary = AgOsColors.n0,
    tertiaryContainer = AgOsColors.warnBg,
    onTertiaryContainer = AgOsColors.warn,
    background = AgOsColors.n50,
    onBackground = AgOsColors.n900,
    surface = AgOsColors.n0,
    onSurface = AgOsColors.n900,
    surfaceVariant = AgOsColors.n100,
    onSurfaceVariant = AgOsColors.n600,
    surfaceTint = AgOsColors.n0,
    surfaceBright = AgOsColors.n0,
    surfaceDim = AgOsColors.n100,
    surfaceContainerLowest = AgOsColors.n0,
    surfaceContainerLow = AgOsColors.n0,
    surfaceContainer = AgOsColors.n0,
    surfaceContainerHigh = AgOsColors.n100,
    surfaceContainerHighest = AgOsColors.n100,
    inverseSurface = AgOsColors.n800,
    inverseOnSurface = AgOsColors.n50,
    outline = AgOsColors.n200,
    outlineVariant = AgOsColors.n100,
    error = AgOsColors.crit,
    onError = AgOsColors.n0,
    errorContainer = AgOsColors.critBg,
    onErrorContainer = AgOsColors.crit,
    scrim = AgOsColors.g950,
)

/** Radii from agos.css: 8 controls, 10 inner, 16 mobile cards, 24 sheets. */
private val TerraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AgOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerraLight,
        typography = AgOsTypography,
        shapes = TerraShapes,
        content = content,
    )
}
