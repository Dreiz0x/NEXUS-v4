package com.nexus.intelligence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.nexus.intelligence.data.local.database.Converters

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["fileType"]),
        Index(value = ["lastModified"]),
        Index(value = ["indexedAt"])
    ]
)
@TypeConverters(Converters::class)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val lastModified: Long,
    val indexedAt: Long = System.currentTimeMillis(),
    val contentPreview: String = "",   // Max 500 chars, siempre disponible
    val parentDirectory: String = "",
    val mimeType: String = "",
    val pageCount: Int = 0,
    val isFromNetwork: Boolean = false,
    val networkSourceDevice: String? = null
    // fullTextContent y embeddingVector movidos a DocumentContentEntity
)

// Tabla separada para contenido pesado — no se jala en queries de listado
@Entity(
    tableName = "document_content",
    indices = [Index(value = ["documentId"], unique = true)]
)
data class DocumentContentEntity(
    @PrimaryKey
    val documentId: Long,
    val fullTextContent: String = "",
    val embeddingVector: String? = null  // JSON float array serializado
)

@Entity(
    tableName = "monitored_folders",
    indices = [Index(value = ["path"], unique = true)]
)
data class MonitoredFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val label: String = "",
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["timestamp"])]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val resultCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "indexing_stats"
)
data class IndexingStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalDocuments: Int = 0,
    val totalPdf: Int = 0,
    val totalWord: Int = 0,
    val totalExcel: Int = 0,
    val totalPowerPoint: Int = 0,
    val totalImages: Int = 0,
    val totalText: Int = 0,
    val totalCsv: Int = 0,
    val lastScanTimestamp: Long = 0,
    val lastScanDurationMs: Long = 0,
    val isCurrentlyScanning: Boolean = false
)
