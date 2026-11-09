package com.doclens.scanner.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.doclens.scanner.core.model.BackupState
import com.doclens.scanner.core.model.LocalSaveState
import com.doclens.scanner.core.model.SessionState

@Entity(
    tableName = "documents",
    indices = [Index(value = ["createdAt"]), Index(value = ["deletedAt"])]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Int,
    val deletedAt: Long?,
    val pageCount: Int,
    val localSaveState: LocalSaveState,
    val backupState: BackupState,
    val lastBackupAt: Long?,
    val isLocked: Boolean = false
)

@Entity(
    tableName = "assets"
)
data class AssetEntity(
    @PrimaryKey val id: String,
    val relativePath: String,
    val mediaType: String,
    val byteSize: Long,
    val checksum: String,
    val width: Int,
    val height: Int,
    val createdAt: Long
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAssetId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["documentId"]), Index(value = ["sourceAssetId"])]
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val position: Int,
    val sourceAssetId: String,
    val recipeJson: String,
    val revision: Int,
    val createdAt: Long
)

@Entity(
    tableName = "capture_sessions",
    indices = [Index(value = ["state"])]
)
data class CaptureSessionEntity(
    @PrimaryKey val id: String,
    val state: SessionState,
    val documentTitle: String?,
    val createdAt: Long,
    val lastActivityAt: Long
)

@Entity(
    tableName = "session_pages",
    primaryKeys = ["sessionId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = CaptureSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["assetId"])]
)
data class SessionPageEntity(
    val sessionId: String,
    val position: Int,
    val assetId: String,
    val recipeJson: String
)

@Entity(
    tableName = "ocr_results",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pageId"]), Index(value = ["documentId"])]
)
data class OcrResultEntity(
    @PrimaryKey val pageId: String,
    val documentId: String,
    val sourceRevision: Int,
    val rawText: String,
    val userReviewedText: String?,
    val blocksJson: String,
    val language: String,
    val updatedAt: Long
)

@Entity(
    tableName = "document_versions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"]), Index(value = ["revision"])]
)
data class DocumentVersionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val revision: Int,
    val pageSnapshotJson: String,
    val reason: String,
    val createdAt: Long
)
