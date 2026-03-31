package com.nexus.intelligence.domain.repository

import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DocumentRepository {
    // Documents
    fun getAllDocuments(): Flow<List<DocumentInfo>>
    fun getRecentDocuments(limit: Int = 50): Flow<List<DocumentInfo>>
    fun getDocumentsByType(type: String): Flow<List<DocumentInfo>>
    fun getDocumentsByDirectory(directory: String): Flow<List<DocumentInfo>>
    fun getDocumentCount(): Flow<Int>
    fun getAllDirectories(): Flow<List<String>>
    suspend fun getDocumentById(id: Long): DocumentInfo?

    // Indexing
    suspend fun indexFile(file: File): DocumentInfo?
    suspend fun removeDeletedFiles()
    suspend fun clearIndex()

    // Search
    suspend fun textSearch(query: String): List<SearchResult>
    suspend fun semanticSearch(query: String): List<SearchResult>
    suspend fun generateEmbeddingsForDocument(docId: Long): Boolean

    // Monitored Folders
    fun getMonitoredFolders(): Flow<List<MonitoredFolderEntity>>
    suspend fun addMonitoredFolder(path: String, label: String = "")
    suspend fun removeMonitoredFolder(path: String)

    // Search History
    suspend fun addSearchHistory(query: String, resultCount: Int)
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    // Stats
    fun getIndexingStats(): Flow<IndexingStatsEntity?>
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)

    // API
    suspend fun isApiAvailable(): Boolean
}
