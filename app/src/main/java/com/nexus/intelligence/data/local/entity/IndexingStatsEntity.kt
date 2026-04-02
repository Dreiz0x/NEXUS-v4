package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indexing_stats")
data class IndexingStatsEntity(
    @PrimaryKey 
    val id: String = "default",
    val totalFiles: Int = 0,
    val indexedFiles: Int = 0,
    val lastScanDurationMs: Long = 0,
    val lastScanTimestamp: Long = 0,
    val isCurrentlyScanning: Boolean = false
)
