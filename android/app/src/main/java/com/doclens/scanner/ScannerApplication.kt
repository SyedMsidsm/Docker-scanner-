package com.doclens.scanner

import android.app.Application
import com.doclens.scanner.core.database.ScannerDatabase
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepository
import com.doclens.scanner.data.repository.DocumentRepositoryImpl
import com.doclens.scanner.engine.image.AndroidImageEngine
import com.doclens.scanner.engine.image.ImageEngine
import com.doclens.scanner.engine.ocr.MlKitOcrEngine
import com.doclens.scanner.engine.ocr.OcrEngine
import com.doclens.scanner.engine.pdf.AndroidPdfEngine
import com.doclens.scanner.engine.pdf.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScannerApplication : Application() {

    lateinit var database: ScannerDatabase
        private set
    lateinit var assetStore: AssetStore
        private set
    lateinit var imageEngine: ImageEngine
        private set
    lateinit var pdfEngine: PdfEngine
        private set
    lateinit var ocrEngine: OcrEngine
        private set
    lateinit var repository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = ScannerDatabase.getInstance(this)
        assetStore = AssetStore(this)
        imageEngine = AndroidImageEngine()
        pdfEngine = AndroidPdfEngine(assetStore, imageEngine)
        ocrEngine = MlKitOcrEngine(this)

        repository = DocumentRepositoryImpl(
            documentDao = database.documentDao(),
            pageDao = database.pageDao(),
            assetDao = database.assetDao(),
            sessionDao = database.captureSessionDao(),
            ocrDao = database.ocrDao(),
            versionDao = database.documentVersionDao(),
            assetStore = assetStore
        )

        // Startup reconciliation for interrupted saves (Section 4.3 rule 9)
        CoroutineScope(Dispatchers.IO).launch {
            assetStore.reconcileInterruptedOperations()
        }
    }
}
