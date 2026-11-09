package com.doclens.scanner.core.model

import java.io.File

data class WatermarkConfig(
    val text: String,
    val opacity: Float = 0.25f, // 0.0 to 1.0
    val rotationDegrees: Float = -45f,
    val fontSizePoints: Float = 36f,
    val isEnabled: Boolean = false
)

data class SignatureConfig(
    val signatureFile: File,
    val normalizedRect: NormalizedRect = NormalizedRect(0.6f, 0.75f, 0.9f, 0.85f),
    val pageIndex: Int = 0 // 0-based page index
)

data class IdCardLayoutConfig(
    val frontAssetId: String,
    val backAssetId: String,
    val purposeWatermark: String? = null,
    val outputTitle: String = "ID_Card_Copy"
)
