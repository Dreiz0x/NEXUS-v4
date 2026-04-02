package com.nexus.intelligence.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.*

@Database(
    entities = [
        DocumentEntity::class,
        DocumentContentEntity::class,
        MonitoredFolderEntity::class,
        SearchHistoryEntity::class,
        IndexingStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Crear tabla document_contents (Plural para coincidir con Entities)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_contents` (
                        `documentId` INTEGER NOT NULL,
                        `fullTextContent` TEXT NOT NULL DEFAULT '',
                        `embeddingVector` TEXT,
                        PRIMARY KEY(`documentId`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_contents_documentId` ON `document_contents` (`documentId`)")

                // 2. Recrear tabla documents para quitar columnas pesadas y añadir de red
                database.execSQL("""
                    CREATE TABLE `documents_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `indexedAt` INTEGER NOT NULL,
                        `contentPreview` TEXT NOT NULL DEFAULT '',
                        `parentDirectory` TEXT NOT NULL DEFAULT '',
                        `mimeType` TEXT NOT NULL DEFAULT '',
                        `pageCount` INTEGER NOT NULL DEFAULT 0,
                        `isFromNetwork` INTEGER NOT NULL DEFAULT 0,
                        `networkSourceDevice` TEXT
                    )
                """)

                database.execSQL("""
                    INSERT INTO documents_new (id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount)
                    SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount FROM documents
                """)

                database.execSQL("DROP TABLE documents")
                database.execSQL("ALTER TABLE documents_new RENAME TO documents")
                
                // Recrear índices
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_documents_filePath` ON `documents` (`filePath`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_fileType` ON `documents` (`fileType`)")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: String?): FloatArray? {
        return value?.removeSurrounding("[", "]")?.split(",")?.filter { it.isNotBlank() }?.map { it.trim().toFloat() }?.toFloatArray()
    }

    @TypeConverter
    fun toFloatArray(array: FloatArray?): String? {
        return array?.joinToString(",", "[", "]")
    }
}
