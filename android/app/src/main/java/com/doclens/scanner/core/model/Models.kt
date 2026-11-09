package com.doclens.scanner.core.model

import java.util.UUID

enum class LocalSaveState {
    SAVING,
    SAVED_LOCALLY,
    ERROR
}

enum class BackupState {
    NOT_BACKED_UP,
    BACKUP_PENDING,
    BACKING_UP,
    BACKED_UP,
    BACKUP_FAILED
}

data class Document(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val folderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val revision: Int = 1,
    val deletedAt: Long? = null,
    val pageCount: Int = 0,
    val localSaveState: LocalSaveState = LocalSaveState.SAVED_LOCALLY,
    val backupState: BackupState = BackupState.NOT_BACKED_UP,
    val lastBackupAt: Long? = null,
    val isLocked: Boolean = false
)

data class NormalizedPoint(
    val x: Float, // 0.0f to 1.0f
    val y: Float  // 0.0f to 1.0f
)

data class CropQuad(
    val topLeft: NormalizedPoint = NormalizedPoint(0f, 0f),
    val topRight: NormalizedPoint = NormalizedPoint(1f, 0f),
    val bottomRight: NormalizedPoint = NormalizedPoint(1f, 1f),
    val bottomLeft: NormalizedPoint = NormalizedPoint(0f, 1f)
) {
    fun isDefault(): Boolean =
        topLeft.x == 0f && topLeft.y == 0f &&
        topRight.x == 1f && topRight.y == 0f &&
        bottomRight.x == 1f && bottomRight.y == 1f &&
        bottomLeft.x == 0f && bottomLeft.y == 1f
}

enum class ImageFilter {
    ORIGINAL,
    COLOR_BOOST,
    GRAYSCALE,
    BW_CLEAN
}

data class EditRecipe(
    val schemaVersion: Int = 1,
    val cropQuad: CropQuad = CropQuad(),
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val filter: ImageFilter = ImageFilter.ORIGINAL,
    val brightness: Float = 0.0f, // -0.5f to 0.5f
    val contrast: Float = 1.0f    // 0.5f to 2.0f
)

data class Page(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val position: Int,
    val sourceAssetId: String,
    val recipe: EditRecipe = EditRecipe(),
    val revision: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

data class Asset(
    val id: String = UUID.randomUUID().toString(),
    val relativePath: String, // e.g. "assets/<id>/original.jpg"
    val mediaType: String = "image/jpeg",
    val byteSize: Long,
    val checksum: String,
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionState {
    ACTIVE,
    COMPLETED,
    DISCARDED
}

data class CaptureSession(
    val id: String = UUID.randomUUID().toString(),
    val state: SessionState = SessionState.ACTIVE,
    val documentTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis()
)

data class SessionPage(
    val sessionId: String,
    val position: Int,
    val assetId: String,
    val recipe: EditRecipe = EditRecipe()
)
