package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val resultCount: Int = 0, // Campo faltante que causaba error
    val timestamp: Long = System.currentTimeMillis()
)
