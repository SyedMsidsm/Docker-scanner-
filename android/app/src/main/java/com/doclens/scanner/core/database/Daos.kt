package com.doclens.scanner.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Update
    suspend fun update(document: DocumentEntity)

    @Query("UPDATE documents SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun permanentDelete(id: String)
}

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    fun observePagesForDocument(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY position ASC")
    suspend fun getPagesForDocument(documentId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getById(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)

    @Update
    suspend fun update(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: String)
}

@Dao
interface AssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: String): AssetEntity?

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT * FROM assets WHERE id NOT IN (SELECT sourceAssetId FROM pages) 
        AND id NOT IN (SELECT assetId FROM session_pages)
    """)
    suspend fun getOrphanAssets(): List<AssetEntity>
}

@Dao
interface CaptureSessionDao {

    @Query("SELECT * FROM capture_sessions WHERE state = 'ACTIVE' ORDER BY lastActivityAt DESC LIMIT 1")
    suspend fun getActiveSession(): CaptureSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CaptureSessionEntity)

    @Update
    suspend fun update(session: CaptureSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionPage(page: SessionPageEntity)

    @Query("SELECT * FROM session_pages WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun getSessionPages(sessionId: String): List<SessionPageEntity>

    @Query("DELETE FROM session_pages WHERE sessionId = :sessionId AND position = :position")
    suspend fun deleteSessionPage(sessionId: String, position: Int)

    @Query("DELETE FROM session_pages WHERE sessionId = :sessionId")
    suspend fun clearSessionPages(sessionId: String)

    @Query("DELETE FROM capture_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
