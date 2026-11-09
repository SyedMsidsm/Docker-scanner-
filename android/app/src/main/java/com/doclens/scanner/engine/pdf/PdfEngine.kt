package com.doclens.scanner.engine.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.doclens.scanner.core.model.IdCardLayoutConfig
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.core.model.PageOcrResult
import com.doclens.scanner.core.model.SignatureConfig
import com.doclens.scanner.core.model.WatermarkConfig
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.engine.image.ImageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

enum class PaperSize {
    A4,
    LETTER,
    ORIGINAL
}

data class PdfExportConfig(
    val title: String,
    val paperSize: PaperSize = PaperSize.A4,
    val quality: Int = 85,
    val maxDimension: Int = 2048,
    val watermark: WatermarkConfig = WatermarkConfig(text = ""),
    val signatures: List<SignatureConfig> = emptyList(),
    val ocrResults: Map<String, PageOcrResult> = emptyMap() // pageId -> OCR result
)

interface PdfEngine {
    suspend fun exportPdf(
        pages: List<Page>,
        outputFile: File,
        config: PdfExportConfig,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): File

    suspend fun exportJpgImages(
        pages: List<Page>,
        outputDirectory: File,
        baseName: String,
        config: PdfExportConfig,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<File>

    suspend fun exportIdCardLayout(
        config: IdCardLayoutConfig,
        outputFile: File,
        quality: Int = 90
    ): File
}

class AndroidPdfEngine(
    private val assetStore: AssetStore,
    private val imageEngine: ImageEngine
) : PdfEngine {

    override suspend fun exportPdf(
        pages: List<Page>,
        outputFile: File,
        config: PdfExportConfig,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): File = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "Cannot export an empty page list" }

        val document = PdfDocument()
        val total = pages.size

        try {
            pages.forEachIndexed { index, page ->
                onProgress?.invoke(index + 1, total)

                val assetFile = assetStore.getAssetFile(page.sourceAssetId)
                val processedBitmap = imageEngine.renderProcessedBitmap(
                    sourceFile = assetFile,
                    recipe = page.recipe,
                    maxDimension = config.maxDimension
                )

                try {
                    val (pageWidth, pageHeight) = calculatePageDimensions(
                        bitmapWidth = processedBitmap.width,
                        bitmapHeight = processedBitmap.height,
                        paperSize = config.paperSize
                    )

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val pdfPage = document.startPage(pageInfo)
                    val canvas: Canvas = pdfPage.canvas

                    // 1. Scale and center scanned image on PDF canvas
                    val scale = min(
                        pageWidth.toFloat() / processedBitmap.width,
                        pageHeight.toFloat() / processedBitmap.height
                    )
                    val dstW = (processedBitmap.width * scale).toInt()
                    val dstH = (processedBitmap.height * scale).toInt()
                    val left = (pageWidth - dstW) / 2
                    val top = (pageHeight - dstH) / 2

                    val dstRect = Rect(left, top, left + dstW, top + dstH)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(processedBitmap, null, dstRect, paint)

                    // 2. Invisible Searchable Text Layer (OCR-03)
                    val ocr = config.ocrResults[page.id]
                    if (ocr != null && ocr.blocks.isNotEmpty()) {
                        drawInvisibleTextLayer(
                            canvas = canvas,
                            ocrResult = ocr,
                            imageLeft = left.toFloat(),
                            imageTop = top.toFloat(),
                            imageWidth = dstW.toFloat(),
                            imageHeight = dstH.toFloat()
                        )
                    }

                    // 3. Electronic Signature Overlay (PDF-06)
                    val pageSignatures = config.signatures.filter { it.pageIndex == index }
                    pageSignatures.forEach { sig ->
                        if (sig.signatureFile.exists()) {
                            drawSignature(
                                canvas = canvas,
                                signatureFile = sig.signatureFile,
                                normalizedRect = sig.normalizedRect,
                                imageLeft = left.toFloat(),
                                imageTop = top.toFloat(),
                                imageWidth = dstW.toFloat(),
                                imageHeight = dstH.toFloat()
                            )
                        }
                    }

                    // 4. Purpose / Security Watermark (PDF-07)
                    if (config.watermark.isEnabled && config.watermark.text.isNotBlank()) {
                        drawWatermark(
                            canvas = canvas,
                            pageWidth = pageWidth,
                            pageHeight = pageHeight,
                            watermark = config.watermark
                        )
                    }

                    document.finishPage(pdfPage)
                } finally {
                    processedBitmap.recycle()
                }
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
                out.flush()
            }

            check(outputFile.exists() && outputFile.length() > 0) {
                "PDF export failed: file is missing or empty"
            }

            outputFile
        } finally {
            document.close()
        }
    }

    override suspend fun exportJpgImages(
        pages: List<Page>,
        outputDirectory: File,
        baseName: String,
        config: PdfExportConfig,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): List<File> = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "Cannot export an empty page list" }
        outputDirectory.mkdirs()

        val total = pages.size
        val exportedFiles = mutableListOf<File>()

        pages.forEachIndexed { index, page ->
            onProgress?.invoke(index + 1, total)

            val assetFile = assetStore.getAssetFile(page.sourceAssetId)
            val outFile = File(outputDirectory, "${baseName}_page_${index + 1}.jpg")

            imageEngine.renderToFile(
                sourceFile = assetFile,
                recipe = page.recipe,
                outputFile = outFile,
                maxDimension = config.maxDimension,
                quality = config.quality
            )

            exportedFiles.add(outFile)
        }

        exportedFiles
    }

    override suspend fun exportIdCardLayout(
        config: IdCardLayoutConfig,
        outputFile: File,
        quality: Int
    ): File = withContext(Dispatchers.IO) {
        // Standard A4 page: 595 x 842 points
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val pdfPage = document.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // Fill page with clean white background
            canvas.drawColor(Color.WHITE)

            val frontFile = assetStore.getAssetFile(config.frontAssetId)
            val backFile = assetStore.getAssetFile(config.backAssetId)

            val frontBmp = BitmapFactory.decodeFile(frontFile.absolutePath)
            val backBmp = BitmapFactory.decodeFile(backFile.absolutePath)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Layout ID cards (standard ID card: 85.6mm x 54mm -> approx 242 x 153 pt)
            val cardW = 340
            val cardH = 215
            val left = (595 - cardW) / 2

            // Front Card on top half (Y: 120 to 335)
            val frontTop = 120
            val frontRect = Rect(left, frontTop, left + cardW, frontTop + cardH)
            if (frontBmp != null) {
                canvas.drawBitmap(frontBmp, null, frontRect, paint)
                frontBmp.recycle()
            }

            // Back Card on bottom half (Y: 440 to 655)
            val backTop = 440
            val backRect = Rect(left, backTop, left + cardW, backTop + cardH)
            if (backBmp != null) {
                canvas.drawBitmap(backBmp, null, backRect, paint)
                backBmp.recycle()
            }

            // Draw card border outlines
            val borderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(RectF(frontRect), borderPaint)
            canvas.drawRect(RectF(backRect), borderPaint)

            // Optional Purpose Watermark across page
            if (!config.purposeWatermark.isNullOrBlank()) {
                val wm = WatermarkConfig(text = config.purposeWatermark, isEnabled = true)
                drawWatermark(canvas, 595, 842, wm)
            }

            document.finishPage(pdfPage)

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
                out.flush()
            }
            outputFile
        } finally {
            document.close()
        }
    }

    private fun drawInvisibleTextLayer(
        canvas: Canvas,
        ocrResult: PageOcrResult,
        imageLeft: Float,
        imageTop: Float,
        imageWidth: Float,
        imageHeight: Float
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.TRANSPARENT // Invisible layer for standard PDF text selection & search
        }

        ocrResult.blocks.forEach { block ->
            block.lines.forEach { line ->
                val lineBox = line.boundingBox
                if (lineBox != null && line.text.isNotBlank()) {
                    val lineLeft = imageLeft + (lineBox.left * imageWidth)
                    val lineTop = imageTop + (lineBox.top * imageHeight)
                    val lineBottom = imageTop + (lineBox.bottom * imageHeight)
                    val lineHeight = (lineBottom - lineTop).coerceAtLeast(8f)

                    textPaint.textSize = lineHeight * 0.85f
                    canvas.drawText(line.text, lineLeft, lineBottom, textPaint)
                }
            }
        }
    }

    private fun drawSignature(
        canvas: Canvas,
        signatureFile: File,
        normalizedRect: com.doclens.scanner.core.model.NormalizedRect,
        imageLeft: Float,
        imageTop: Float,
        imageWidth: Float,
        imageHeight: Float
    ) {
        val sigBmp = BitmapFactory.decodeFile(signatureFile.absolutePath) ?: return
        try {
            val sigLeft = imageLeft + (normalizedRect.left * imageWidth)
            val sigTop = imageTop + (normalizedRect.top * imageHeight)
            val sigRight = imageLeft + (normalizedRect.right * imageWidth)
            val sigBottom = imageTop + (normalizedRect.bottom * imageHeight)

            val dst = RectF(sigLeft, sigTop, sigRight, sigBottom)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sigBmp, null, dst, paint)
        } finally {
            sigBmp.recycle()
        }
    }

    private fun drawWatermark(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        watermark: WatermarkConfig
    ) {
        canvas.save()
        val centerX = pageWidth / 2f
        val centerY = pageHeight / 2f
        canvas.rotate(watermark.rotationDegrees, centerX, centerY)

        val alpha = (watermark.opacity * 255).toInt().coerceIn(10, 255)
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 180, 0, 0)
            textSize = watermark.fontSizePoints
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        canvas.drawText(watermark.text, centerX, centerY, wmPaint)
        canvas.restore()
    }

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        paperSize: PaperSize
    ): Pair<Int, Int> {
        return when (paperSize) {
            PaperSize.A4 -> {
                if (bitmapWidth > bitmapHeight) 842 to 595 else 595 to 842
            }
            PaperSize.LETTER -> {
                if (bitmapWidth > bitmapHeight) 792 to 612 else 612 to 792
            }
            PaperSize.ORIGINAL -> {
                bitmapWidth to bitmapHeight
            }
        }
    }
}
