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
}

                
    
