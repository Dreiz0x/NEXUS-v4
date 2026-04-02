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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val isFromNetwork: Int = 0, // Agregado para coincidir con la migración
    val networkSourceDevice: String? = null // Agregado para coincidir con la migración
)

@Entity(
    tableName = "document_contents", // SIEMPRE PLURAL
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentContentEntity(
    @PrimaryKey
    val documentId: Long, // Usamos el ID del documento como PK para evitar confusiones
    val fullTextContent: String = "",
    val embeddingVector: String? = null
)
