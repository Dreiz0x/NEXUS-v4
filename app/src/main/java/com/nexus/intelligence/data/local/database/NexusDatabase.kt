package com.nexus.intelligence.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.DocumentContentEntity
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentContentEntity::class,   // Nueva tabla para contenido pesado
        MonitoredFolderEntity::class,
        SearchHistoryEntity::class,
        IndexingStatsEntity::class
    ],
    version = 2,                        // Bumped de 1 a 2
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "nexus_intelligence_db"

        // Migración 1→2:
        // - Crea tabla document_content con fullTextContent y embeddingVector
        // - Mueve los datos existentes
        // - Recrea documents sin las columnas pesadas
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Crear nueva tabla de contenido
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_content` (
                        `documentId` INTEGER NOT NULL,
                        `fullTextContent` TEXT NOT NULL DEFAULT '',
                        `embeddingVector` TEXT,
                        PRIMARY KEY(`documentId`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_document_content_documentId` ON `document_content` (`documentId`)"
                )

                // 2. Migrar contenido existente a la nueva tabla
                database.execSQL("""
                    INSERT INTO document_content (documentId, fullTextContent, embeddingVector)
                    SELECT id, COALESCE(fullTextContent, ''), embeddingVector
                    FROM documents
                    WHERE fullTextContent IS NOT NULL AND fullTextContent != ''
                """)

                // 3. Recrear documents sin columnas pesadas
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
                    INSERT INTO documents_new 
                    SELECT id, filePath, fileName, fileType, fileSize, lastModified,
                           indexedAt, contentPreview, parentDirectory, mimeType,
                           pageCount, isFromNetwork, networkSourceDevice
                    FROM documents
                """)

                database.execSQL("DROP TABLE documents")
                database.execSQL("ALTER TABLE documents_new RENAME TO documents")

                // Recrear índices
                database.execSQL("CREATE UNIQUE INDEX `index_documents_filePath` ON `documents` (`filePath`)")
                database.execSQL("CREATE INDEX `index_documents_fileType` ON `documents` (`fileType`)")
                database.execSQL("CREATE INDEX `index_documents_lastModified` ON `documents` (`lastModified`)")
                database.execSQL("CREATE INDEX `index_documents_indexedAt` ON `documents` (`indexedAt`)")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: String?): FloatArray? {
        if (value == null) return null
        return try {
            value.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toFloatArray(array: FloatArray?): String? {
        return array?.joinToString(",", "[", "]")
    }
}
