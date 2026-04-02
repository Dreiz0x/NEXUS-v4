package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["fileType"]),
        Index(value = ["lastModified"]),
        Index(value = ["indexedAt"])
    ]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val lastModified: Long,
    val indexedAt: Long = System.currentTimeMillis(),
    val contentPreview: String = "",
    val parentDirectory: String = "",
    val mimeType: String = "",
    val pageCount: Int = 0,
    val isFromNetwork: Int = 0,
    val networkSourceDevice: String? = null
)
