package com.doclens.scanner.core.model

import android.graphics.RectF

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toAbsoluteRectF(width: Float, height: Float): RectF {
        return RectF(
            left * width,
            top * height,
            right * width,
            bottom * height
        )
    }

    companion object {
        fun fromAbsolute(rect: RectF, width: Float, height: Float): NormalizedRect {
            val w = width.coerceAtLeast(1f)
            val h = height.coerceAtLeast(1f)
            return NormalizedRect(
                left = (rect.left / w).coerceIn(0f, 1f),
                top = (rect.top / h).coerceIn(0f, 1f),
                right = (rect.right / w).coerceIn(0f, 1f),
                bottom = (rect.bottom / h).coerceIn(0f, 1f)
            )
        }
    }
}

data class OcrWord(
    val text: String,
    val boundingBox: NormalizedRect? = null,
    val confidence: Float? = null // Null if engine does not supply confidence (Section 28.8)
)

data class OcrLine(
    val text: String,
    val boundingBox: NormalizedRect? = null,
    val words: List<OcrWord> = emptyList()
)

data class OcrBlock(
    val text: String,
    val boundingBox: NormalizedRect? = null,
    val lines: List<OcrLine> = emptyList()
)

data class PageOcrResult(
    val pageId: String,
    val sourceRevision: Int,
    val rawText: String,
    val userReviewedText: String? = null,
    val blocks: List<OcrBlock> = emptyList(),
    val language: String = "und",
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns user-reviewed text if present, otherwise raw recognized text.
     */
    val effectiveText: String
        get() = userReviewedText ?: rawText
}
