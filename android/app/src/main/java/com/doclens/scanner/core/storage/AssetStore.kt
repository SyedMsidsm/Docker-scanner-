package com.doclens.scanner.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.doclens.scanner.core.model.Asset
import com.doclens.scanner.core.model.EditRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

class AssetStore(private val context: Context) {

    private val filesDir: File = context.filesDir
    private val cacheDir: File = context.cacheDir

    private val assetsDir: File = File(filesDir, "assets").apply { mkdirs() }
    private val stagingDir: File = File(filesDir, "staging").apply { mkdirs() }
    private val thumbnailsDir: File = File(cacheDir, "thumbnails").apply { mkdirs() }
    private val exportDir: File = File(cacheDir, "temporary-share-exports").apply { mkdirs() }

    /**
     * Staged save protocol (Section 4.3)
     */
    suspend fun storeOriginalAsset(inputStream: InputStream): Asset = withContext(Dispatchers.IO) {
        val assetId = UUID.randomUUID().toString()
        val opId = UUID.randomUUID().toString()

        val stageDir = File(stagingDir, opId).apply { mkdirs() }
        val stagingFile = File(stageDir, "upload.tmp")

        try {
            // 1 & 2: Stream source to staging file
            FileOutputStream(stagingFile).use { out ->
                inputStream.copyTo(out)
                out.flush()
            }

            // 3: Compute SHA-256 checksum
            val checksum = computeSha256(stagingFile)
            val byteSize = stagingFile.length()

            // Read dimensions without loading full bitmap into memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(stagingFile.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight

            // 4 & 5: Atomic move to durable asset location
            val targetDir = File(assetsDir, assetId).apply { mkdirs() }
            val targetFile = File(targetDir, "original.jpg")

            val moved = stagingFile.renameTo(targetFile)
            if (!moved) {
                // Fallback copy if renameTo fails
                stagingFile.copyTo(targetFile, overwrite = true)
                stagingFile.delete()
            }

            val relativePath = "assets/$assetId/original.jpg"

            Asset(
                id = assetId,
                relativePath = relativePath,
                mediaType = "image/jpeg",
                byteSize = byteSize,
                checksum = checksum,
                width = width,
                height = height,
                createdAt = System.currentTimeMillis()
            )
        } finally {
            stagingFile.delete()
            stageDir.delete()
        }
    }

    suspend fun storeOriginalFromBytes(bytes: ByteArray): Asset = withContext(Dispatchers.IO) {
        bytes.inputStream().use { storeOriginalAsset(it) }
    }

    fun getAssetFile(assetId: String): File {
        return File(assetsDir, "$assetId/original.jpg")
    }

    fun getThumbnailFile(assetId: String, recipe: EditRecipe): File {
        val recipeHash = recipe.hashCode()
        return File(thumbnailsDir, "${assetId}_${recipeHash}.jpg")
    }

    fun createTemporaryExportFile(filename: String): File {
        exportDir.mkdirs()
        return File(exportDir, filename)
    }

    fun clearTemporaryExports() {
        exportDir.listFiles()?.forEach { it.delete() }
    }

    fun reconcileInterruptedOperations() {
        stagingDir.listFiles()?.forEach { dir ->
            dir.deleteRecursively()
        }
    }

    fun deleteAssetFiles(assetId: String) {
        File(assetsDir, assetId).deleteRecursively()
        thumbnailsDir.listFiles { _, name -> name.startsWith(assetId) }?.forEach { it.delete() }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
