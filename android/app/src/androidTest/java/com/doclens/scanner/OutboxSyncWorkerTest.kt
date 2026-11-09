package com.doclens.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.doclens.scanner.core.database.DocumentEntity
import com.doclens.scanner.core.database.OutboxOperationEntity
import com.doclens.scanner.core.database.ScannerDatabase
import com.doclens.scanner.core.model.BackupState
import com.doclens.scanner.core.model.ContentNetworkMode
import com.doclens.scanner.core.model.LocalSaveState
import com.doclens.scanner.core.security.PrivacyManager
import com.doclens.scanner.core.storage.AssetStore
import com.doclens.scanner.data.repository.DocumentRepositoryImpl
import com.doclens.scanner.data.sync.SyncWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OutboxSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: ScannerDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = ScannerDatabase.getInstance(context)
    }

    @Test
    fun testLocalOnlyPrivacyGateBlocksNetworkSync() = runBlocking {
        // Enforce Local-Only Privacy Mode (IMP-05)
        PrivacyManager.setNetworkMode(context, ContentNetworkMode.LOCAL_ONLY)

        val outboxDao = database.outboxDao()
        val documentDao = database.documentDao()

        val docId = UUID.randomUUID().toString()
        val doc = DocumentEntity(
            id = docId,
            title = "Top Secret Scan",
            folderId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            revision = 1,
            deletedAt = null,
            pageCount = 1,
            localSaveState = LocalSaveState.SAVED_LOCALLY,
            backupState = BackupState.NOT_BACKED_UP,
            lastBackupAt = null,
            isLocked = false
        )
        documentDao.insert(doc)

        val opId = UUID.randomUUID().toString()
        val op = OutboxOperationEntity(
            id = opId,
            documentId = docId,
            operationType = "CREATE",
            baseRevision = 1,
            payloadJson = "{\"title\":\"Top Secret Scan\"}",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        outboxDao.insert(op)

        // Execute Worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // Verify: outbox operation must NOT have been uploaded or cleared
        val remainingOps = outboxDao.getPendingOperations().filter { it.id == opId }
        assertEquals("Outbox operation must remain pending when local-only is enabled", 1, remainingOps.size)

        // Verify: document must remain truthfully NOT_BACKED_UP
        val docAfter = documentDao.getById(docId)
        assertNotNull(docAfter)
        assertEquals(BackupState.NOT_BACKED_UP, docAfter?.backupState)

        // Cleanup
        outboxDao.delete(opId)
        documentDao.permanentDelete(docId)
    }

    @Test
    fun testSyncWorkerAcknowledgeAndTransition() = runBlocking {
        // Allow cloud sync
        PrivacyManager.setNetworkMode(context, ContentNetworkMode.CLOUD_ENABLED)

        val outboxDao = database.outboxDao()
        val documentDao = database.documentDao()

        val docId = UUID.randomUUID().toString()
        val doc = DocumentEntity(
            id = docId,
            title = "Synced Cloud Document",
            folderId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            revision = 1,
            deletedAt = null,
            pageCount = 1,
            localSaveState = LocalSaveState.SAVED_LOCALLY,
            backupState = BackupState.NOT_BACKED_UP,
            lastBackupAt = null,
            isLocked = false
        )
        documentDao.insert(doc)

        val opId = UUID.randomUUID().toString()
        val op = OutboxOperationEntity(
            id = opId,
            documentId = docId,
            operationType = "CREATE",
            baseRevision = 1,
            payloadJson = "{\"title\":\"Synced Cloud Document\"}",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        outboxDao.insert(op)

        // Execute Worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // Verify: Outbox operation cleared
        val remainingOps = outboxDao.getPendingOperations().filter { it.id == opId }
        assertEquals("Outbox operation should be cleared upon successful commit", 0, remainingOps.size)

        // Verify: Document transitioned to BACKED_UP
        val updatedDoc = documentDao.getById(docId)
        assertNotNull(updatedDoc)
        assertEquals(BackupState.BACKED_UP, updatedDoc?.backupState)
        assertTrue("lastBackupAt timestamp must be set", (updatedDoc?.lastBackupAt ?: 0) > 0)

        // Cleanup
        documentDao.permanentDelete(docId)
    }
}
