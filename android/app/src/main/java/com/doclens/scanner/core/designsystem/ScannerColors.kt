package com.doclens.scanner.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ScannerColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val borderSubtle: Color,
    val success: Color,
    val warningAccent: Color,
    val warningText: Color,
    val error: Color,
    val outlineStrong: Color,
    val canvasBackground: Color,
    val isDark: Boolean
)

val LocalScannerColors = staticCompositionLocalOf {
    ScannerColors(
        primary = ColorTokens.DarkPrimary,
        onPrimary = ColorTokens.DarkOnPrimary,
        background = ColorTokens.DarkBackground,
        surface = ColorTokens.DarkSurface,
        surfaceVariant = ColorTokens.DarkSurfaceVariant,
        onSurface = ColorTokens.DarkOnSurface,
        onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
        borderSubtle = ColorTokens.DarkBorderSubtle,
        success = ColorTokens.DarkSuccess,
        warningAccent = ColorTokens.DarkWarningAccent,
        warningText = ColorTokens.DarkWarningText,
        error = ColorTokens.DarkError,
        outlineStrong = ColorTokens.DarkOutlineStrong,
        canvasBackground = ColorTokens.CanvasBackgroundDark,
        isDark = true
    )
}
