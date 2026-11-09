package com.doclens.scanner.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.doclens.scanner.core.model.AppTextSize

@Immutable
data class ScannerTypography(
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val compactBody: TextStyle,
    val buttonLabel: TextStyle,
    val supportingLabel: TextStyle
)

fun createScannerTypography(textSize: AppTextSize = AppTextSize.DEFAULT): ScannerTypography {
    val m = textSize.multiplier

    fun scaledSp(base: Float): TextUnit = (base * m).sp

    return ScannerTypography(
        screenTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = scaledSp(30f),
            lineHeight = scaledSp(38f)
        ),
        sectionTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(22f),
            lineHeight = scaledSp(30f)
        ),
        cardTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledSp(20f),
            lineHeight = scaledSp(28f)
        ),
        body = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(16f),
            lineHeight = scaledSp(24f)
        ),
        compactBody = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(14f),
            lineHeight = scaledSp(20f)
        ),
        buttonLabel = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = scaledSp(14f),
            lineHeight = scaledSp(20f)
        ),
        supportingLabel = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = scaledSp(12f),
            lineHeight = scaledSp(18f)
        )
    )
}

val LocalScannerTypography = staticCompositionLocalOf {
    createScannerTypography(AppTextSize.DEFAULT)
}
