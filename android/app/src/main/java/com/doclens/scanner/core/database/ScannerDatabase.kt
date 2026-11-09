package com.doclens.scanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DocumentEntity::class,
        AssetEntity::class,
        PageEntity::class,
        CaptureSessionEntity::class,
        SessionPageEntity::class,
        OcrResultEntity::class,
        DocumentVersionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ScannerDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun assetDao(): AssetDao
    abstract fun captureSessionDao(): CaptureSessionDao
    abstract fun ocrDao(): OcrDao
    abstract fun documentVersionDao(): DocumentVersionDao

    companion object {
        private const val DB_NAME = "scanner_documents.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ocr_results` (
                        `pageId` TEXT NOT NULL,
                        `documentId` TEXT NOT NULL,
                        `sourceRevision` INTEGER NOT NULL,
                        `rawText` TEXT NOT NULL,
                        `userReviewedText` TEXT,
                        `blocksJson` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`pageId`),
                        FOREIGN KEY(`pageId`) REFERENCES `pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_results_pageId` ON `ocr_results` (`pageId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_results_documentId` ON `ocr_results` (`documentId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_versions` (
                        `id` TEXT NOT NULL,
                        `documentId` TEXT NOT NULL,
                        `revision` INTEGER NOT NULL,
                        `pageSnapshotJson` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_versions_documentId` ON `document_versions` (`documentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_versions_revision` ON `document_versions` (`revision`)")
            }
        }

        @Volatile
        private var instance: ScannerDatabase? = null

        fun getInstance(context: Context): ScannerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
        }
    }
}
