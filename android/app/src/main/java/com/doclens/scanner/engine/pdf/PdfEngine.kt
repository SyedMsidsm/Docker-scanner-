package com.doclens.scanner.engine.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.doclens.scanner.core.model.Page
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
    val maxDimension: Int = 2048
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

                    // Scale and center image on PDF page
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

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        paperSize: PaperSize
    ): Pair<Int, Int> {
        return when (paperSize) {
            PaperSize.A4 -> {
                // A4 at 72 points per inch
                if (bitmapWidth > bitmapHeight) {
                    842 to 595 // Landscape
                } else {
                    595 to 842 // Portrait
                }
            }
            PaperSize.LETTER -> {
                // US Letter at 72 points per inch
                if (bitmapWidth > bitmapHeight) {
                    792 to 612 // Landscape
                } else {
                    612 to 792 // Portrait
                }
            }
            PaperSize.ORIGINAL -> {
                bitmapWidth to bitmapHeight
            }
        }
    }
}
