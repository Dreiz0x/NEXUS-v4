package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_folders")
data class MonitoredFolderEntity(
    @PrimaryKey val id: String,
    val path: String,
    val label: String = "", // Campo faltante que causaba error
    val addedAt: Long = System.currentTimeMillis()
)
