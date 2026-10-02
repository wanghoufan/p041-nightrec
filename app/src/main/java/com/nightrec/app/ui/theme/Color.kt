package com.nightrec.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand gradient stops: violet -> magenta/pink -> orange/yellow.
// Source: design/ 视觉资产使用规范 + 自适应前景 SVG (#6D28FF / #FF35B5 / #FFB11B).
val BrandViolet = Color(0xFF6D28FF)
val BrandMagenta = Color(0xFFFF35B5)
val BrandOrange = Color(0xFFFFB11B)

// Reserved for emphasis, waveform, key CTAs only — never large body fills.
val BrandGradient = listOf(BrandViolet, BrandMagenta, BrandOrange)

fun brandBrush(): Brush = Brush.linearGradient(BrandGradient)

// Splash / system background tokens (must match values/styles.xml + values-night).
val SplashLightBackground = Color(0xFFF7F4FF)
val SplashDarkBackground = Color(0xFF0B0B14)

// ---- Light scheme: near-white / light purple background + dark text ----
internal val LightColors = androidx.compose.material3.lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DCFF),
    onPrimaryContainer = Color(0xFF24005B),
    secondary = Color(0xFFC2189A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD7F0),
    onSecondaryContainer = Color(0xFF3A002C),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color(0xFFFFFFFF),
    background = SplashLightBackground,
    onBackground = Color(0xFF1A1726),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1726),
    surfaceVariant = Color(0xFFEDE7FB),
    onSurfaceVariant = Color(0xFF494459),
    outline = Color(0xFFC9C2DA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

// ---- Dark scheme: near-black indigo background + white / light-gray text ----
internal val DarkColors = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFB79CFF),
    onPrimary = Color(0xFF2A0B6B),
    primaryContainer = Color(0xFF43179E),
    onPrimaryContainer = Color(0xFFE7DCFF),
    secondary = Color(0xFFFF7DD2),
    onSecondary = Color(0xFF580041),
    secondaryContainer = Color(0xFF7B005C),
    onSecondaryContainer = Color(0xFFFFD7F0),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF4F2A00),
    background = SplashDarkBackground,
    onBackground = Color(0xFFEDEAF5),
    surface = Color(0xFF14131F),
    onSurface = Color(0xFFEDEAF5),
    surfaceVariant = Color(0xFF232134),
    onSurfaceVariant = Color(0xFFC9C4DA),
    outline = Color(0xFF4A4760),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)