package com.doclens.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doclens.scanner.core.database.ScannerDatabase
import com.doclens.scanner.core.model.NormalizedRect
import com.doclens.scanner.core.model.OcrBlock
import com.doclens.scanner.core.model.OcrLine
import com.doclens.scanner.core.model.OcrWord
import com.doclens.scanner.core.model.PageOcrResult
import com.doclens.scanner.core.model.SignatureConfig
import com.doclens.scanner.core.model.WatermarkConfig
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepositoryImpl
import com.doclens.scanner.engine.image.AndroidImageEngine
import com.doclens.scanner.engine.pdf.AndroidPdfEngine
import com.doclens.scanner.engine.pdf.PaperSize
import com.doclens.scanner.engine.pdf.PdfExportConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class OcrAndAdvancedPdfTest {

    @Test
    fun testOcrStorageAndSearchablePdfGeneration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = ScannerDatabase.getInstance(context)
        val assetStore = AssetStore(context)
        val imageEngine = AndroidImageEngine()
        val pdfEngine = AndroidPdfEngine(assetStore, imageEngine)

        val repository = DocumentRepositoryImpl(
            documentDao = database.documentDao(),
            pageDao = database.pageDao(),
            assetDao = database.assetDao(),
            sessionDao = database.captureSessionDao(),
            ocrDao = database.ocrDao(),
            versionDao = database.documentVersionDao(),
            assetStore = assetStore
        )

        // 1. Create a document with 1 page containing printed text
        val session = repository.startNewSession("OCR Benchmark Doc")

        val pageBitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(pageBitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
        }
        canvas.drawText("TAX INVOICE 2026", 100f, 150f, paint)
        canvas.drawText("Total Due: Rs 4,500.00", 100f, 250f, paint)

        val stream = ByteArrayOutputStream()
        pageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        pageBitmap.recycle()

        val asset = assetStore.storeOriginalFromBytes(stream.toByteArray())
        repository.addAssetToSession(session.id, asset)
        val doc = repository.commitSessionToDocument(session.id, "OCR Document")

        val pages = repository.getPages(doc.id)
        assertEquals(1, pages.size)
        val pageId = pages[0].id

        // 2. Save OCR Result to Database
        val ocrResult = PageOcrResult(
            pageId = pageId,
            sourceRevision = 1,
            rawText = "TAX INVOICE 2026\nTotal Due: Rs 4,500.00",
            userReviewedText = null,
            blocks = listOf(
                OcrBlock(
                    text = "TAX INVOICE 2026",
                    boundingBox = NormalizedRect(0.12f, 0.10f, 0.60f, 0.16f),
                    lines = listOf(
                        OcrLine(
                            text = "TAX INVOICE 2026",
                            boundingBox = NormalizedRect(0.12f, 0.10f, 0.60f, 0.16f),
                            words = listOf(
                                OcrWord("TAX"),
                                OcrWord("INVOICE"),
                                OcrWord("2026")
                            )
                        )
                    )
                ),
                OcrBlock(
                    text = "Total Due: Rs 4,500.00",
                    boundingBox = NormalizedRect(0.12f, 0.20f, 0.70f, 0.26f),
                    lines = listOf(
                        OcrLine(
                            text = "Total Due: Rs 4,500.00",
                            boundingBox = NormalizedRect(0.12f, 0.20f, 0.70f, 0.26f),
                            words = listOf(
                                OcrWord("Total"),
                                OcrWord("Due:"),
                                OcrWord("Rs"),
                                OcrWord("4,500.00")
                            )
                        )
                    )
                )
            ),
            language = "en"
        )
        repository.saveOcrResult(doc.id, ocrResult)

        // Verify OCR retrieved from DB
        val retrievedOcr = repository.getOcrResult(pageId)
        assertNotNull(retrievedOcr)
        assertEquals("TAX INVOICE 2026\nTotal Due: Rs 4,500.00", retrievedOcr!!.rawText)

        // 3. User Review / Human Correction (Section 28.8: human corrections never overwritten)
        repository.updateUserReviewedText(pageId, "TAX INVOICE 2026 (Verified Correct)")
        val reviewedOcr = repository.getOcrResult(pageId)
        assertEquals("TAX INVOICE 2026 (Verified Correct)", reviewedOcr!!.userReviewedText)
        assertEquals("TAX INVOICE 2026 (Verified Correct)", reviewedOcr.effectiveText)

        // 4. Create Electronic Signature file
        val sigFile = File(context.cacheDir, "test_signature.png")
        val sigBitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val sigCanvas = Canvas(sigBitmap)
        sigCanvas.drawColor(Color.TRANSPARENT)
        val sigPaint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        sigCanvas.drawLine(10f, 50f, 180f, 50f, sigPaint)
        FileOutputStream(sigFile).use { out ->
            sigBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        sigBitmap.recycle()

        // 5. Export Searchable PDF with Invisible Text Layer, Watermark, and Signature
        val exportFile = assetStore.createTemporaryExportFile("searchable_signed_watermarked.pdf")
        val pdfConfig = PdfExportConfig(
            title = doc.title,
            paperSize = PaperSize.A4,
            watermark = WatermarkConfig(text = "CONFIDENTIAL BENCHMARK", isEnabled = true),
            signatures = listOf(SignatureConfig(signatureFile = sigFile, pageIndex = 0)),
            ocrResults = mapOf(pageId to reviewedOcr)
        )

        val resultPdf = pdfEngine.exportPdf(
            pages = pages,
            outputFile = exportFile,
            config = pdfConfig
        )

        assertTrue(resultPdf.exists())
        assertTrue(resultPdf.length() > 5000) // Valid PDF bytes including text layer and signature
    }
}
