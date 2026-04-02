package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "monitored_folders",
    indices = [Index(value = ["path"], unique = true)]
)
data class MonitoredFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val label: String = "",
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
