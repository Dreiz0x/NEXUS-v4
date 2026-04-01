package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indexing_stats")
data class IndexingStatsEntity(
    @PrimaryKey
    val id: String = "default",
    val totalDocuments: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)
