package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    val indexedAt: Long = System.currentTimeMillis(),
    val contentPreview: String = "",
    val parentDirectory: String = "",
    val mimeType: String = "application/pdf",
    val pageCount: Int = 0,
    val hasEmbedding: Boolean = false
)

/**
 * Tabla separada para el contenido completo de los documentos.
 * El uso de ForeignKey con CASCADE asegura que si borras un documento, 
 * su contenido se limpie automáticamente de la base de datos.
 */
@Entity(
    tableName = "document_contents",
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val fullTextContent: String
)
