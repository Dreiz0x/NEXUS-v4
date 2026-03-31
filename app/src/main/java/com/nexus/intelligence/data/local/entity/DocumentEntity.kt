package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val lastModified: Long,
    val indexedAt: Long,
    val contentPreview: String,
    val parentDirectory: String,
    val mimeType: String,
    val pageCount: Int,
    val isFromNetwork: Boolean = false,
    val networkSourceDevice: String? = null
)
