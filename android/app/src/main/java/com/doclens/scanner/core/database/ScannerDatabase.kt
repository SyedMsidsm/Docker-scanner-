package com.doclens.scanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DocumentEntity::class,
        AssetEntity::class,
        PageEntity::class,
        CaptureSessionEntity::class,
        SessionPageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ScannerDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun assetDao(): AssetDao
    abstract fun captureSessionDao(): CaptureSessionDao

    companion object {
        private const val DB_NAME = "scanner_documents.db"

        @Volatile
        private var instance: ScannerDatabase? = null

        fun getInstance(context: Context): ScannerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
        }
    }
}
