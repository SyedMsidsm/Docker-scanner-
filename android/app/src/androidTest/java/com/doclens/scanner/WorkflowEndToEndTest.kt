package com.doclens.scanner

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doclens.scanner.core.database.ScannerDatabase
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.NormalizedPoint
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

@RunWith(AndroidJUnit4::class)
class WorkflowEndToEndTest {

    @Test
    fun testTenPageWorkflowCaptureDurableSaveReorderRecoverExport() = runBlocking {
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

        // 1. Capture/Import 10 genuine JPEG pages into an active session
        val session = repository.startNewSession("10-Page Test Document")
        assertNotNull(session)

        for (i in 1..10) {
            val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            bitmap.recycle()

            val asset = assetStore.storeOriginalFromBytes(stream.toByteArray())
            assertEquals("assets/${asset.id}/original.jpg", asset.relativePath)
            assertTrue(asset.byteSize > 0)
            assertEquals(64, asset.checksum.length) // Valid SHA-256

            val pageCount = repository.addAssetToSession(session.id, asset)
            assertEquals(i, pageCount)
        }

        // 2. Durable Save: Commit session to document
        val document = repository.commitSessionToDocument(session.id, "Ten Page Verified Document")
        assertEquals(10, document.pageCount)

        // Verify all 10 pages exist in Room DB
        val pages = repository.getPages(document.id)
        assertEquals(10, pages.size)
        pages.forEachIndexed { index, page ->
            assertEquals(index, page.position)
        }

        // 3. Crop / Rotate Edits on Page 1
        val page1 = pages[0]
        val cropRecipe = EditRecipe(
            cropQuad = CropQuad(
                topLeft = NormalizedPoint(0.05f, 0.05f),
                topRight = NormalizedPoint(0.95f, 0.05f),
                bottomRight = NormalizedPoint(0.95f, 0.95f),
                bottomLeft = NormalizedPoint(0.05f, 0.95f)
            ),
            rotationDegrees = 90,
            filter = ImageFilter.BW_CLEAN,
            brightness = 0.1f,
            contrast = 1.2f
        )
        repository.updatePageRecipe(page1.id, cropRecipe)

        val updatedPage1 = repository.getPages(document.id)[0]
        assertEquals(90, updatedPage1.recipe.rotationDegrees)
        assertEquals(ImageFilter.BW_CLEAN, updatedPage1.recipe.filter)

        // 4. Reorder: Move Page 10 to Position 0
        val currentIds = pages.map { it.id }.toMutableList()
        val lastId = currentIds.removeAt(9)
        currentIds.add(0, lastId)
        repository.reorderPages(document.id, currentIds)

        val reorderedPages = repository.getPages(document.id)
        assertEquals(lastId, reorderedPages[0].id)
        assertEquals(0, reorderedPages[0].position)

        // 5. Restart & Recover: Re-query database from fresh repository instance
        val freshRepository = DocumentRepositoryImpl(
            documentDao = database.documentDao(),
            pageDao = database.pageDao(),
            assetDao = database.assetDao(),
            sessionDao = database.captureSessionDao(),
            ocrDao = database.ocrDao(),
            versionDao = database.documentVersionDao(),
            assetStore = assetStore
        )
        val recoveredDoc = freshRepository.getDocument(document.id)
        assertNotNull(recoveredDoc)
        assertEquals(10, recoveredDoc!!.pageCount)

        val recoveredPages = freshRepository.getPages(document.id)
        assertEquals(10, recoveredPages.size)
        assertEquals(lastId, recoveredPages[0].id)

        // 6. Export PDF & Validate Actual Output Bytes
        val exportFile = assetStore.createTemporaryExportFile("test_ten_page_export.pdf")
        var progressCount = 0

        val resultPdf = pdfEngine.exportPdf(
            pages = recoveredPages,
            outputFile = exportFile,
            config = PdfExportConfig(
                title = recoveredDoc.title,
                paperSize = PaperSize.A4
            ),
            onProgress = { cur, tot ->
                progressCount = cur
                assertEquals(10, tot)
            }
        )

        assertTrue("Exported PDF file must exist", resultPdf.exists())
        assertTrue("Exported PDF byte size must be greater than zero", resultPdf.length() > 1024)
        assertEquals(10, progressCount)

        // Also test JPG Export
        val jpgDir = assetStore.createTemporaryExportFile("test_jpg_export_dir")
        val exportedJpgs = pdfEngine.exportJpgImages(
            pages = recoveredPages,
            outputDirectory = jpgDir,
            baseName = "test_export",
            config = PdfExportConfig(title = "JPG Export")
        )
        assertEquals(10, exportedJpgs.size)
        exportedJpgs.forEach { jpg ->
            assertTrue(jpg.exists())
            assertTrue(jpg.length() > 0)
        }
    }
}
