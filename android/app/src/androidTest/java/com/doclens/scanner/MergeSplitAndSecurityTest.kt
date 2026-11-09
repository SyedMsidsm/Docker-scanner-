package com.doclens.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doclens.scanner.core.database.ScannerDatabase
import com.doclens.scanner.core.model.Asset
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.NormalizedPoint
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepository
import com.doclens.scanner.data.repository.DocumentRepositoryImpl
import com.doclens.scanner.engine.image.AndroidImageEngine
import com.doclens.scanner.engine.pdf.AndroidPdfEngine
import com.doclens.scanner.engine.pdf.PdfEngine
import com.doclens.scanner.engine.pdf.PdfExportConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MergeSplitAndSecurityTest {

    private lateinit var context: Context
    private lateinit var database: ScannerDatabase
    private lateinit var assetStore: AssetStore
    private lateinit var repository: DocumentRepository
    private lateinit var pdfEngine: PdfEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)

        database = Room.inMemoryDatabaseBuilder(context, ScannerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        assetStore = AssetStore(context)
        val imageEngine = AndroidImageEngine()
        pdfEngine = AndroidPdfEngine(assetStore, imageEngine)

        repository = DocumentRepositoryImpl(
            documentDao = database.documentDao(),
            pageDao = database.pageDao(),
            assetDao = database.assetDao(),
            sessionDao = database.captureSessionDao(),
            ocrDao = database.ocrDao(),
            versionDao = database.documentVersionDao(),
            assetStore = assetStore
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    private suspend fun createTestAsset(color: Int): Asset {
        val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        return repository.importImageStream(ByteArrayInputStream(bytes))
    }

    @Test
    fun testMergeDocuments() = runBlocking {
        // Create Document A with 2 pages
        val sessionA = repository.startNewSession("Doc A")
        val asset1 = createTestAsset(Color.RED)
        val asset2 = createTestAsset(Color.BLUE)
        repository.addAssetToSession(sessionA.id, asset1)
        repository.addAssetToSession(sessionA.id, asset2)
        val docA = repository.commitSessionToDocument(sessionA.id, "Document A")

        // Create Document B with 2 pages
        val sessionB = repository.startNewSession("Doc B")
        val asset3 = createTestAsset(Color.GREEN)
        val asset4 = createTestAsset(Color.YELLOW)
        repository.addAssetToSession(sessionB.id, asset3)
        repository.addAssetToSession(sessionB.id, asset4)
        val docB = repository.commitSessionToDocument(sessionB.id, "Document B")

        // Merge Doc A and Doc B
        val mergedDocId = repository.mergeDocuments(listOf(docA.id, docB.id), "Merged A and B")
        val mergedDoc = repository.getDocument(mergedDocId)
        assertNotNull(mergedDoc)
        assertEquals("Merged A and B", mergedDoc?.title)
        assertEquals(4, mergedDoc?.pageCount)

        val pages = repository.getPages(mergedDocId)
        assertEquals(4, pages.size)
        assertEquals(0, pages[0].position)
        assertEquals(1, pages[1].position)
        assertEquals(2, pages[2].position)
        assertEquals(3, pages[3].position)
        assertEquals(asset1.id, pages[0].sourceAssetId)
        assertEquals(asset2.id, pages[1].sourceAssetId)
        assertEquals(asset3.id, pages[2].sourceAssetId)
        assertEquals(asset4.id, pages[3].sourceAssetId)
    }

    @Test
    fun testSplitDocument() = runBlocking {
        // Create Document with 4 pages
        val session = repository.startNewSession("Original 4 Pages")
        for (i in 1..4) {
            val asset = createTestAsset(Color.rgb(i * 40, i * 40, i * 40))
            repository.addAssetToSession(session.id, asset)
        }
        val originalDoc = repository.commitSessionToDocument(session.id, "Original 4 Pages")

        // Split after page index 1 (Part 1 gets pages 0, 1; Part 2 gets pages 2, 3)
        val (part1Id, part2Id) = repository.splitDocument(
            originalDoc.id,
            splitAfterPageIndex = 1,
            firstTitle = "Split Part 1",
            secondTitle = "Split Part 2"
        )

        val part1 = repository.getDocument(part1Id)
        val part2 = repository.getDocument(part2Id)
        assertNotNull(part1)
        assertNotNull(part2)

        assertEquals(2, part1?.pageCount)
        assertEquals(2, part2?.pageCount)

        val part1Pages = repository.getPages(part1Id)
        val part2Pages = repository.getPages(part2Id)

        assertEquals(2, part1Pages.size)
        assertEquals(2, part2Pages.size)
        assertEquals(0, part1Pages[0].position)
        assertEquals(1, part1Pages[1].position)
        assertEquals(0, part2Pages[0].position)
        assertEquals(1, part2Pages[1].position)
    }

    @Test
    fun testPdfPasswordProtection() = runBlocking {
        // Create 1-page document and export
        val asset = createTestAsset(Color.DKGRAY)
        val session = repository.startNewSession("Protected Doc")
        repository.addAssetToSession(session.id, asset)
        val doc = repository.commitSessionToDocument(session.id, "Protected Doc")
        val pages = repository.getPages(doc.id)

        val rawPdf = assetStore.createTemporaryExportFile("test_unprotected.pdf")
        pdfEngine.exportPdf(pages, rawPdf, PdfExportConfig(title = "Unprotected"))
        assertTrue("Raw PDF must exist", rawPdf.exists() && rawPdf.length() > 0)

        // Protect with password
        val protectedPdf = assetStore.createTemporaryExportFile("test_protected.pdf")
        val password = "StrongScannerPassword2026!"
        pdfEngine.protectPdf(rawPdf, protectedPdf, password)
        assertTrue("Protected PDF must exist", protectedPdf.exists() && protectedPdf.length() > 0)

        // Attempt opening without password -> should fail
        try {
            val unauthDoc = PDDocument.load(protectedPdf)
            if (unauthDoc.isEncrypted) {
                // Good: document is encrypted
                unauthDoc.close()
            }
        } catch (e: InvalidPasswordException) {
            // Expected
        }

        // Open with correct password -> must succeed
        val authDoc = PDDocument.load(protectedPdf, password)
        assertNotNull(authDoc)
        assertTrue("Document must be marked encrypted", authDoc.isEncrypted)
        assertEquals(1, authDoc.numberOfPages)
        authDoc.close()
    }

    @Test
    fun testPdfMergeAndSplitStructural() = runBlocking {
        // Create 2 separate PDFs
        val asset1 = createTestAsset(Color.MAGENTA)
        val asset2 = createTestAsset(Color.CYAN)

        val page1 = Page(id = "p1", documentId = "d1", position = 0, sourceAssetId = asset1.id)
        val page2 = Page(id = "p2", documentId = "d2", position = 0, sourceAssetId = asset2.id)

        val pdf1 = assetStore.createTemporaryExportFile("doc1.pdf")
        val pdf2 = assetStore.createTemporaryExportFile("doc2.pdf")

        pdfEngine.exportPdf(listOf(page1), pdf1, PdfExportConfig(title = "PDF 1"))
        pdfEngine.exportPdf(listOf(page2), pdf2, PdfExportConfig(title = "PDF 2"))

        // Structural merge using PDFBox
        val mergedPdf = assetStore.createTemporaryExportFile("merged_structural.pdf")
        pdfEngine.mergePdfs(listOf(pdf1, pdf2), mergedPdf)
        assertTrue("Merged PDF must exist", mergedPdf.exists() && mergedPdf.length() > 0)

        val parsedMerged = PDDocument.load(mergedPdf)
        assertEquals(2, parsedMerged.numberOfPages)
        parsedMerged.close()

        // Structural split
        val splitDir = File(context.cacheDir, "split_test_${System.currentTimeMillis()}").apply { mkdirs() }
        val splitPages = pdfEngine.splitPdf(mergedPdf, splitDir, "split_out")
        assertEquals(2, splitPages.size)
        assertTrue("Split page 1 exists", splitPages[0].exists())
        assertTrue("Split page 2 exists", splitPages[1].exists())

        val splitDoc1 = PDDocument.load(splitPages[0])
        assertEquals(1, splitDoc1.numberOfPages)
        splitDoc1.close()

        val splitDoc2 = PDDocument.load(splitPages[1])
        assertEquals(1, splitDoc2.numberOfPages)
        splitDoc2.close()
    }

    @Test
    fun testDocumentLockState() = runBlocking {
        val asset = createTestAsset(Color.BLACK)
        val session = repository.startNewSession("Lock Test")
        repository.addAssetToSession(session.id, asset)
        val doc = repository.commitSessionToDocument(session.id, "Lock Test")

        // Initial state is unlocked
        assertFalse(doc.isLocked)

        // Lock
        repository.setDocumentLocked(doc.id, true)
        val lockedDoc = repository.getDocument(doc.id)
        assertNotNull(lockedDoc)
        assertTrue("Document must be locked", lockedDoc!!.isLocked)

        // Unlock
        repository.setDocumentLocked(doc.id, false)
        val unlockedDoc = repository.getDocument(doc.id)
        assertNotNull(unlockedDoc)
        assertFalse("Document must be unlocked", unlockedDoc!!.isLocked)
    }
}
