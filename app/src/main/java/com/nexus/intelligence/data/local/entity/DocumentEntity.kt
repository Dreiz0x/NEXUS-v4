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
    val indexedAt: Long = System.currentTimeMillis(),
    val contentPreview: String = "",
    val parentDirectory: String = "",
    val mimeType: String = "application/pdf",
    val pageCount: Int = 0,
    val hasEmbedding: Boolean = false
)

/**
 * Tabla separada para el contenido completo de los documentos.
 * Esto evita que las consultas de listas sean lentas al no cargar el texto pesado.
 */
@Entity(tableName = "document_contents")
data class DocumentContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long, // Relación con el ID de DocumentEntity
    val fullTextContent: String
)
