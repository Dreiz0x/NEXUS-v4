package com.nexus.intelligence.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Crear tabla document_contents
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

                // 2. Mover datos pesados
                database.execSQL("""
                    INSERT INTO document_contents (documentId, fullTextContent, embeddingVector)
                    SELECT id, fullTextContent, embeddingVector FROM documents
                """)

                // 3. (Opcional) solo si tu SDK ≥ 31 y SQLite ≥ 3.35
                // database.execSQL("ALTER TABLE documents DROP COLUMN fullTextContent")
                // database.execSQL("ALTER TABLE documents DROP COLUMN embeddingVector")
                // Si no, solo déjalas vacías. No tocas la estructura de `documents`.
            }
        }
    }
}
