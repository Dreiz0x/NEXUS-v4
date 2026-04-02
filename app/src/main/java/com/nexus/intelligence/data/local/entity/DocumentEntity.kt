package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Entity(
    tableName = "document_contents", // USAMOS PLURAL PARA COINCIDIR CON TU DAO
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class DocumentContentEntity(
    @PrimaryKey val documentId: Long, // El ID del doc es la PK, así no necesitas un 'id' extra
    val fullTextContent: String = "",
    val embeddingVector: String? = null
)
