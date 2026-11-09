package com.doclens.scanner.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.doclens.scanner.core.database.AssetEntity
import com.doclens.scanner.core.database.CaptureSessionEntity
import com.doclens.scanner.core.database.DocumentDao
import com.doclens.scanner.core.database.DocumentEntity
import com.doclens.scanner.core.database.PageDao
import com.doclens.scanner.core.database.PageEntity
import com.doclens.scanner.core.database.AssetDao
import com.doclens.scanner.core.database.CaptureSessionDao
import com.doclens.scanner.core.database.SessionPageEntity
import com.doclens.scanner.core.database.Converters
import com.doclens.scanner.core.model.Asset
import com.doclens.scanner.core.model.BackupState
import com.doclens.scanner.core.model.CaptureSession
import com.doclens.scanner.core.model.Document
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.LocalSaveState
import com.doclens.scanner.core.model.Page
import com.doclens.scanner.core.model.SessionPage
import com.doclens.scanner.core.model.SessionState
import com.doclens.scanner.core.storage.AssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

interface DocumentRepository {
    fun observeDocuments(): Flow<List<Document>>
    fun observeDocument(id: String): Flow<Document?>
    fun observePages(documentId: String): Flow<List<Page>>
    suspend fun getDocument(id: String): Document?
    suspend fun getPages(documentId: String): List<Page>
    suspend fun updateDocumentTitle(documentId: String, title: String)
    suspend fun softDeleteDocument(documentId: String)

    suspend fun getActiveSession(): CaptureSession?
    suspend fun getSessionPages(sessionId: String): List<SessionPage>
    suspend fun startNewSession(title: String? = null): CaptureSession
    suspend fun addAssetToSession(sessionId: String, asset: Asset, recipe: EditRecipe = EditRecipe()): Int
    suspend fun removeSessionPage(sessionId: String, position: Int)
    suspend fun discardSession(sessionId: String)
    suspend fun commitSessionToDocument(sessionId: String, title: String): Document

    suspend fun reorderPages(documentId: String, newOrderedPageIds: List<String>)
    suspend fun updatePageRecipe(pageId: String, recipe: EditRecipe)
    suspend fun duplicatePage(pageId: String): Page
    suspend fun deletePage(pageId: String)
    suspend fun appendPageToDocument(documentId: String, asset: Asset, recipe: EditRecipe = EditRecipe()): Page

    suspend fun importImageStream(inputStream: InputStream): Asset
    suspend fun importPdfStream(context: Context, inputStream: InputStream, sessionId: String): Int
}

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val assetDao: AssetDao,
    private val sessionDao: CaptureSessionDao,
    private val assetStore: AssetStore
) : DocumentRepository {

    private val converters = Converters()

    override fun observeDocuments(): Flow<List<Document>> {
        return documentDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeDocument(id: String): Flow<Document?> {
        return documentDao.observeById(id).map { it?.toDomain() }
    }

    override fun observePages(documentId: String): Flow<List<Page>> {
        return pageDao.observePagesForDocument(documentId).map { list ->
            list.map { it.toDomain(converters) }
        }
    }

    override suspend fun getDocument(id: String): Document? = withContext(Dispatchers.IO) {
        documentDao.getById(id)?.toDomain()
    }

    override suspend fun getPages(documentId: String): List<Page> = withContext(Dispatchers.IO) {
        pageDao.getPagesForDocument(documentId).map { it.toDomain(converters) }
    }

    override suspend fun updateDocumentTitle(documentId: String, title: String) = withContext(Dispatchers.IO) {
        val existing = documentDao.getById(documentId) ?: return@withContext
        documentDao.update(existing.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun softDeleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        documentDao.softDelete(documentId)
    }

    override suspend fun getActiveSession(): CaptureSession? = withContext(Dispatchers.IO) {
        sessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun getSessionPages(sessionId: String): List<SessionPage> = withContext(Dispatchers.IO) {
        sessionDao.getSessionPages(sessionId).map {
            SessionPage(
                sessionId = it.sessionId,
                position = it.position,
                assetId = it.assetId,
                recipe = converters.toEditRecipe(it.recipeJson)
            )
        }
    }

    override suspend fun startNewSession(title: String?): CaptureSession = withContext(Dispatchers.IO) {
        // Mark any existing active session as discarded or return it
        val session = CaptureSession(
            id = UUID.randomUUID().toString(),
            state = SessionState.ACTIVE,
            documentTitle = title
        )
        sessionDao.insert(session.toEntity())
        session
    }

    override suspend fun addAssetToSession(
        sessionId: String,
        asset: Asset,
        recipe: EditRecipe
    ): Int = withContext(Dispatchers.IO) {
        // Record asset in DB
        assetDao.insert(asset.toEntity())

        val currentPages = sessionDao.getSessionPages(sessionId)
        val nextPosition = currentPages.size

        val sessionPage = SessionPageEntity(
            sessionId = sessionId,
            position = nextPosition,
            assetId = asset.id,
            recipeJson = converters.fromEditRecipe(recipe)
        )
        sessionDao.insertSessionPage(sessionPage)

        val session = sessionDao.getActiveSession()
        if (session != null) {
            sessionDao.update(session.copy(lastActivityAt = System.currentTimeMillis()))
        }

        nextPosition + 1
    }

    override suspend fun removeSessionPage(sessionId: String, position: Int) = withContext(Dispatchers.IO) {
        sessionDao.deleteSessionPage(sessionId, position)
        // Renumber remaining pages
        val remaining = sessionDao.getSessionPages(sessionId).sortedBy { it.position }
        sessionDao.clearSessionPages(sessionId)
        remaining.forEachIndexed { idx, page ->
            sessionDao.insertSessionPage(page.copy(position = idx))
        }
    }

    override suspend fun discardSession(sessionId: String) = withContext(Dispatchers.IO) {
        sessionDao.deleteSession(sessionId)
    }

    override suspend fun commitSessionToDocument(sessionId: String, title: String): Document = withContext(Dispatchers.IO) {
        val sessionPages = sessionDao.getSessionPages(sessionId).sortedBy { it.position }
        require(sessionPages.isNotEmpty()) { "Cannot commit an empty session" }

        val docId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val document = Document(
            id = docId,
            title = title.ifBlank { "Scan ${System.currentTimeMillis()}" },
            createdAt = now,
            updatedAt = now,
            revision = 1,
            pageCount = sessionPages.size,
            localSaveState = LocalSaveState.SAVED_LOCALLY,
            backupState = BackupState.NOT_BACKED_UP
        )

        documentDao.insert(document.toEntity())

        val pageEntities = sessionPages.mapIndexed { index, sp ->
            PageEntity(
                id = UUID.randomUUID().toString(),
                documentId = docId,
                position = index,
                sourceAssetId = sp.assetId,
                recipeJson = sp.recipeJson,
                revision = 1,
                createdAt = now
            )
        }
        pageDao.insertAll(pageEntities)

        // Close session
        sessionDao.deleteSession(sessionId)

        document
    }

    override suspend fun reorderPages(
        documentId: String,
        newOrderedPageIds: List<String>
    ) = withContext(Dispatchers.IO) {
        val existing = pageDao.getPagesForDocument(documentId)
        val map = existing.associateBy { it.id }

        newOrderedPageIds.forEachIndexed { newIndex, id ->
            val page = map[id]
            if (page != null && page.position != newIndex) {
                pageDao.update(page.copy(position = newIndex))
            }
        }

        val doc = documentDao.getById(documentId)
        if (doc != null) {
            documentDao.update(doc.copy(revision = doc.revision + 1, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun updatePageRecipe(
        pageId: String,
        recipe: EditRecipe
    ) = withContext(Dispatchers.IO) {
        val page = pageDao.getById(pageId) ?: return@withContext
        pageDao.update(
            page.copy(
                recipeJson = converters.fromEditRecipe(recipe),
                revision = page.revision + 1
            )
        )

        val doc = documentDao.getById(page.documentId)
        if (doc != null) {
            documentDao.update(doc.copy(revision = doc.revision + 1, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun duplicatePage(pageId: String): Page = withContext(Dispatchers.IO) {
        val page = pageDao.getById(pageId) ?: throw IllegalArgumentException("Page not found")
        val existingPages = pageDao.getPagesForDocument(page.documentId)

        val newPageId = UUID.randomUUID().toString()
        val newPosition = page.position + 1

        // Shift subsequent pages
        existingPages.filter { it.position >= newPosition }.forEach {
            pageDao.update(it.copy(position = it.position + 1))
        }

        val duplicate = page.copy(
            id = newPageId,
            position = newPosition,
            revision = 1,
            createdAt = System.currentTimeMillis()
        )
        pageDao.insert(duplicate)

        val doc = documentDao.getById(page.documentId)
        if (doc != null) {
            documentDao.update(
                doc.copy(
                    pageCount = doc.pageCount + 1,
                    revision = doc.revision + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        duplicate.toDomain(converters)
    }

    override suspend fun deletePage(pageId: String) = withContext(Dispatchers.IO) {
        val page = pageDao.getById(pageId) ?: return@withContext
        val docId = page.documentId
        pageDao.deleteById(pageId)

        // Renumber remaining pages
        val remaining = pageDao.getPagesForDocument(docId).sortedBy { it.position }
        remaining.forEachIndexed { index, p ->
            if (p.position != index) {
                pageDao.update(p.copy(position = index))
            }
        }

        val doc = documentDao.getById(docId)
        if (doc != null) {
            documentDao.update(
                doc.copy(
                    pageCount = remaining.size,
                    revision = doc.revision + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun appendPageToDocument(
        documentId: String,
        asset: Asset,
        recipe: EditRecipe
    ): Page = withContext(Dispatchers.IO) {
        assetDao.insert(asset.toEntity())
        val existing = pageDao.getPagesForDocument(documentId)
        val position = existing.size

        val page = PageEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            position = position,
            sourceAssetId = asset.id,
            recipeJson = converters.fromEditRecipe(recipe),
            revision = 1,
            createdAt = System.currentTimeMillis()
        )
        pageDao.insert(page)

        val doc = documentDao.getById(documentId)
        if (doc != null) {
            documentDao.update(
                doc.copy(
                    pageCount = position + 1,
                    revision = doc.revision + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        page.toDomain(converters)
    }

    override suspend fun importImageStream(inputStream: InputStream): Asset = withContext(Dispatchers.IO) {
        assetStore.storeOriginalAsset(inputStream)
    }

    override suspend fun importPdfStream(
        context: Context,
        inputStream: InputStream,
        sessionId: String
    ): Int = withContext(Dispatchers.IO) {
        // Save stream to a temp file for PdfRenderer
        val tempPdf = File.createTempFile("imported_", ".pdf", context.cacheDir)
        try {
            FileOutputStream(tempPdf).use { out ->
                inputStream.copyTo(out)
                out.flush()
            }

            val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            var count = 0

            try {
                for (i in 0 until renderer.pageCount) {
                    val pdfPage = renderer.openPage(i)
                    // Render at high resolution (approx 150-200 dpi)
                    val scale = 2.0f
                    val w = (pdfPage.width * scale).toInt()
                    val h = (pdfPage.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pdfPage.close()

                    val byteStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, byteStream)
                    bitmap.recycle()

                    val asset = assetStore.storeOriginalFromBytes(byteStream.toByteArray())
                    addAssetToSession(sessionId, asset)
                    count++
                }
            } finally {
                renderer.close()
                pfd.close()
            }

            count
        } finally {
            tempPdf.delete()
        }
    }
}

private fun DocumentEntity.toDomain() = Document(
    id = id,
    title = title,
    folderId = folderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    deletedAt = deletedAt,
    pageCount = pageCount,
    localSaveState = localSaveState,
    backupState = backupState,
    lastBackupAt = lastBackupAt
)

private fun Document.toEntity() = DocumentEntity(
    id = id,
    title = title,
    folderId = folderId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    revision = revision,
    deletedAt = deletedAt,
    pageCount = pageCount,
    localSaveState = localSaveState,
    backupState = backupState,
    lastBackupAt = lastBackupAt
)

private fun PageEntity.toDomain(converters: Converters) = Page(
    id = id,
    documentId = documentId,
    position = position,
    sourceAssetId = sourceAssetId,
    recipe = converters.toEditRecipe(recipeJson),
    revision = revision,
    createdAt = createdAt
)

private fun AssetEntity.toDomain() = Asset(
    id = id,
    relativePath = relativePath,
    mediaType = mediaType,
    byteSize = byteSize,
    checksum = checksum,
    width = width,
    height = height,
    createdAt = createdAt
)

private fun Asset.toEntity() = AssetEntity(
    id = id,
    relativePath = relativePath,
    mediaType = mediaType,
    byteSize = byteSize,
    checksum = checksum,
    width = width,
    height = height,
    createdAt = createdAt
)

private fun CaptureSessionEntity.toDomain() = CaptureSession(
    id = id,
    state = state,
    documentTitle = documentTitle,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt
)

private fun CaptureSession.toEntity() = CaptureSessionEntity(
    id = id,
    state = state,
    documentTitle = documentTitle,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt
)
