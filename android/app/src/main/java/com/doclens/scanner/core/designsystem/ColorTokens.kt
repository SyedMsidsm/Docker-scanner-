package com.doclens.scanner.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Exact palette tokens defined in Section 30 of BUILD_GUIDE.md.
 */
object ColorTokens {
    // Dark Palette (Default)
    val DarkBackground = Color(0xFF111411)
    val DarkSurface = Color(0xFF1C211D)
    val DarkSurfaceVariant = Color(0xFF262C27)
    val DarkPrimary = Color(0xFFD9AC46) // Gold
    val DarkOnPrimary = Color(0xFF111411)
    val DarkOnSurface = Color(0xFFF6F2E9)
    val DarkOnSurfaceVariant = Color(0xFFB9C0B8)
    val DarkBorderSubtle = Color(0xFF354038)
    val DarkSuccess = Color(0xFF75B890)
    val DarkWarningAccent = Color(0xFFE4B765)
    val DarkWarningText = Color(0xFFE4B765) // Meets >= 4.5:1 on dark surface
    val DarkError = Color(0xFFCF6679)
    val DarkOutlineStrong = Color(0xFF78877C)

    // Light Palette
    val LightBackground = Color(0xFFF6F7F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFEBEFEA)
    val LightPrimary = Color(0xFF8A6014) // Deep warm gold
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightOnSurface = Color(0xFF141915)
    val LightOnSurfaceVariant = Color(0xFF47524A)
    val LightBorderSubtle = Color(0xFFCCD4CD)
    val LightSuccess = Color(0xFF1B5E20)
    val LightWarningAccent = Color(0xFFB76E00)
    val LightWarningText = Color(0xFF8A5200) // Meets >= 4.5:1 on light surface
    val LightError = Color(0xFFBA1A1A)
    val LightOutlineStrong = Color(0xFF718076)

    // Document canvas neutral colors (MUST NEVER recolor document scans)
    val CanvasBackgroundDark = Color(0xFF0A0C0A)
    val CanvasBackgroundLight = Color(0xFFE2E4E0)
    val PaperWhite = Color(0xFFFFFFFF)
}
