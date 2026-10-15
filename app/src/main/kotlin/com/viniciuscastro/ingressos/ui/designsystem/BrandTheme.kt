package com.viniciuscastro.ingressos.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Brand typeface. The identity uses DM Sans; until the font files are bundled in `res/font` the
 * platform sans-serif is used, and swapping it is a one-line change here.
 */
val BrandFont: FontFamily = FontFamily.SansSerif

private val BrandColorScheme = lightColorScheme(
    primary = BrandColors.Cielo,
    onPrimary = BrandColors.Surface,
    primaryContainer = BrandColors.Sky,
    onPrimaryContainer = BrandColors.CieloDark,
    secondary = BrandColors.CieloDark,
    onSecondary = BrandColors.Surface,
    secondaryContainer = BrandColors.Sky,
    onSecondaryContainer = BrandColors.CieloDark,
    tertiary = BrandColors.Warning,
    onTertiary = BrandColors.Surface,
    tertiaryContainer = BrandColors.WarningSoft,
    onTertiaryContainer = BrandColors.Warning,
    error = BrandColors.Danger,
    onError = BrandColors.Surface,
    errorContainer = BrandColors.DangerSoft,
    onErrorContainer = BrandColors.Danger,
    background = BrandColors.Canvas,
    onBackground = BrandColors.Ink,
    surface = BrandColors.Surface,
    onSurface = BrandColors.Ink,
    surfaceVariant = BrandColors.Canvas,
    onSurfaceVariant = BrandColors.Muted,
    surfaceContainerLowest = BrandColors.Surface,
    surfaceContainerLow = BrandColors.Surface,
    surfaceContainer = BrandColors.Surface,
    surfaceContainerHigh = BrandColors.Surface,
    surfaceContainerHighest = BrandColors.Canvas,
    inverseSurface = BrandColors.Ink,
    inverseOnSurface = BrandColors.Surface,
    inversePrimary = BrandColors.Sky,
    outline = BrandColors.SecondaryBorder,
    outlineVariant = BrandColors.Line,
)

private fun style(size: Int, weight: FontWeight, lineHeight: Float = 1.3f, tracking: Double = 0.0) = TextStyle(
    fontFamily = BrandFont,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
)

/** Scale taken from the identity's CSS (display 34, detail 27, result 25, section 18, header 17…). */
private val BrandTypography = Typography(
    displaySmall = style(34, FontWeight.Bold, lineHeight = 1.08f, tracking = -0.037),
    headlineMedium = style(27, FontWeight.Bold, lineHeight = 1.15f, tracking = -0.026),
    headlineSmall = style(25, FontWeight.Bold, lineHeight = 1.2f, tracking = -0.02),
    titleLarge = style(18, FontWeight.Bold, tracking = -0.014),
    titleMedium = style(17, FontWeight.Bold),
    titleSmall = style(15, FontWeight.Bold),
    bodyLarge = style(15, FontWeight.Normal),
    bodyMedium = style(13, FontWeight.Normal, lineHeight = 1.5f),
    bodySmall = style(11, FontWeight.Normal, lineHeight = 1.4f),
    labelLarge = style(14, FontWeight.Bold),
    labelMedium = style(12, FontWeight.SemiBold),
    labelSmall = style(10, FontWeight.Bold, tracking = 0.05),
)

private val BrandShapes = Shapes(
    small = RoundedCornerShape(BrandRadius.Small),
    medium = RoundedCornerShape(BrandRadius.Medium),
    large = RoundedCornerShape(BrandRadius.Large),
)

/** Light-only on purpose: the identity defines a single, high-contrast palette for the terminal. */
@Composable
fun IngressosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandColorScheme,
        typography = BrandTypography,
        shapes = BrandShapes,
        content = content,
    )
}
