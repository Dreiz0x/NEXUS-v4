package com.nexus.intelligence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── NEXUS Color Palette ──────────────────────────────────────────

object NexusColors {
    val Black = Color(0xFF000000)
    val DeepBlack = Color(0xFF0A0A0F)
    val DarkSurface = Color(0xFF0D0D14)
    val CardBackground = Color(0xFF111118)
    val CardBorder = Color(0xFF1A1A2E)

    val Cyan = Color(0xFF00FFFF)
    val CyanDim = Color(0xFF00CCCC)
    val CyanGlow = Color(0x4000FFFF)
    val CyanSubtle = Color(0x1A00FFFF)

    val Green = Color(0xFF39FF14)
    val GreenDim = Color(0xFF2ECC0F)
    val GreenGlow = Color(0x4039FF14)

    val Magenta = Color(0xFFFF00FF)
    val MagentaDim = Color(0xFFCC00CC)
    val MagentaGlow = Color(0x40FF00FF)

    val Amber = Color(0xFFFFBF00)
    val AmberDim = Color(0xFFCC9900)

    val Red = Color(0xFFFF0040)
    val RedDim = Color(0xFFCC0033)
    val RedGlow = Color(0x40FF0040)

    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFF8888AA)
    val TextDim = Color(0xFF555577)

    val GridLine = Color(0x1A00FFFF)
    val ScanLine = Color(0x0D00FFFF)

    // Document type colors
    val PdfColor = Color(0xFFFF4444)
    val WordColor = Color(0xFF4488FF)
    val ExcelColor = Color(0xFF44CC44)
    val PowerPointColor = Color(0xFFFF8844)
    val ImageColor = Color(0xFFCC44FF)
    val TextColor = Color(0xFF88CCCC)
    val CsvColor = Color(0xFFCCCC44)
}

// ── Typography ───────────────────────────────────────────────────

// Using default monospace font family (system monospace)
val NexusMonospace = FontFamily.Monospace

val NexusTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 4.sp,
        color = NexusColors.Cyan
    ),
    displayMedium = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 3.sp,
        color = NexusColors.Cyan
    ),
    displaySmall = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 2.sp,
        color = NexusColors.Cyan
    ),
    headlineLarge = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 2.sp,
        color = NexusColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 1.5.sp,
        color = NexusColors.TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
        color = NexusColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        color = NexusColors.Cyan
    ),
    titleMedium = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.8.sp,
        color = NexusColors.TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = NexusColors.TextSecondary
    ),
    bodyLarge = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = NexusColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
        color = NexusColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.2.sp,
        color = NexusColors.TextDim
    ),
    labelLarge = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        color = NexusColors.Cyan
    ),
    labelMedium = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = NexusColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = NexusMonospace,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        letterSpacing = 0.5.sp,
        color = NexusColors.TextDim
    )
)

// ── Material3 Color Scheme ───────────────────────────────────────

private val NexusDarkColorScheme = darkColorScheme(
    primary = NexusColors.Cyan,
    onPrimary = NexusColors.Black,
    primaryContainer = NexusColors.CyanSubtle,
    onPrimaryContainer = NexusColors.Cyan,
    secondary = NexusColors.Green,
    onSecondary = NexusColors.Black,
    secondaryContainer = NexusColors.GreenGlow,
    onSecondaryContainer = NexusColors.Green,
    tertiary = NexusColors.Magenta,
    onTertiary = NexusColors.Black,
    tertiaryContainer = NexusColors.MagentaGlow,
    onTertiaryContainer = NexusColors.Magenta,
    error = NexusColors.Red,
    onError = NexusColors.Black,
    errorContainer = NexusColors.RedGlow,
    onErrorContainer = NexusColors.Red,
    background = NexusColors.Black,
    onBackground = NexusColors.TextPrimary,
    surface = NexusColors.DeepBlack,
    onSurface = NexusColors.TextPrimary,
    surfaceVariant = NexusColors.DarkSurface,
    onSurfaceVariant = NexusColors.TextSecondary,
    outline = NexusColors.CardBorder,
    outlineVariant = NexusColors.GridLine
)

// ── Theme Composable ─────────────────────────────────────────────

@Composable
fun NexusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        typography = NexusTypography,
        content = content
    )
}
