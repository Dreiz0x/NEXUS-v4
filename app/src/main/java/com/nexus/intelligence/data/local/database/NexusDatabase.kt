package com.nexus.intelligence.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.intelligence.data.local.dao.DocumentDao
// IMPORTA LAS ENTIDADES UNA POR UNA PARA FORZAR A KSP A ENCONTRARLAS
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.DocumentContentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity

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
        // Mantenemos la migración pero asegúrate de que el SQL use 'document_contents' (plural)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `document_contents` (
                        `documentId` INTEGER NOT NULL,
                        `fullTextContent` TEXT NOT NULL DEFAULT '',
                        `embeddingVector` TEXT,
                        PRIMARY KEY(`documentId`),
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON DELETE CASCADE
                    )
                """)
                // ... resto de tu lógica de recrear tablas ...
            }
        }
    }
}
