package com.doclens.scanner.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.doclens.scanner.core.model.AppTextSize
import com.doclens.scanner.core.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = ColorTokens.DarkPrimary,
    onPrimary = ColorTokens.DarkOnPrimary,
    background = ColorTokens.DarkBackground,
    onBackground = ColorTokens.DarkOnSurface,
    surface = ColorTokens.DarkSurface,
    onSurface = ColorTokens.DarkOnSurface,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
    outline = ColorTokens.DarkOutlineStrong,
    outlineVariant = ColorTokens.DarkBorderSubtle,
    error = ColorTokens.DarkError,
    onError = ColorTokens.DarkOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ColorTokens.LightPrimary,
    onPrimary = ColorTokens.LightOnPrimary,
    background = ColorTokens.LightBackground,
    onBackground = ColorTokens.LightOnSurface,
    surface = ColorTokens.LightSurface,
    onSurface = ColorTokens.LightOnSurface,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurfaceVariant = ColorTokens.LightOnSurfaceVariant,
    outline = ColorTokens.LightOutlineStrong,
    outlineVariant = ColorTokens.LightBorderSubtle,
    error = ColorTokens.LightError,
    onError = ColorTokens.LightOnPrimary
)

private val DarkScannerColors = ScannerColors(
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

private val LightScannerColors = ScannerColors(
    primary = ColorTokens.LightPrimary,
    onPrimary = ColorTokens.LightOnPrimary,
    background = ColorTokens.LightBackground,
    surface = ColorTokens.LightSurface,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurface = ColorTokens.LightOnSurface,
    onSurfaceVariant = ColorTokens.LightOnSurfaceVariant,
    borderSubtle = ColorTokens.LightBorderSubtle,
    success = ColorTokens.LightSuccess,
    warningAccent = ColorTokens.LightWarningAccent,
    warningText = ColorTokens.LightWarningText,
    error = ColorTokens.LightError,
    outlineStrong = ColorTokens.LightOutlineStrong,
    canvasBackground = ColorTokens.CanvasBackgroundLight,
    isDark = false
)

@Composable
fun ScannerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    textSize: AppTextSize = AppTextSize.DEFAULT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val scannerColors = if (isDark) DarkScannerColors else LightScannerColors
    val typography = createScannerTypography(textSize)

    CompositionLocalProvider(
        LocalScannerColors provides scannerColors,
        LocalScannerTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

object ScannerTheme {
    val colors: ScannerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalScannerColors.current

    val typography: ScannerTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalScannerTypography.current
}
