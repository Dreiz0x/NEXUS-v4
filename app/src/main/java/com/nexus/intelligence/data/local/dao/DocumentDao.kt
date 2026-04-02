package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE filePath = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY lastModified DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE fileType = :type")
    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE parentDirectory = :directory")
    fun getDocumentsByDirectory(directory: String): Flow<List<DocumentEntity>>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT DISTINCT parentDirectory FROM documents")
    fun getAllDirectories(): Flow<List<String>>

    @Query("SELECT filePath FROM documents")
    suspend fun getAllFilePaths(): List<String>

    @Query("DELETE FROM documents WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT * FROM documents WHERE fileName LIKE '%' || :query || '%' OR contentPreview LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    // Content
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Query("SELECT * FROM document_contents WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("DELETE FROM document_contents")
    suspend fun deleteAllDocumentContent()

    // Monitored Folders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredFolder(folder: MonitoredFolderEntity)

    @Query("SELECT * FROM monitored_folders")
    fun getAllMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    @Query("DELETE FROM monitored_folders WHERE path = :path")
    suspend fun deleteMonitoredFolderByPath(path: String)

    // Search History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(history: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    // Stats
    @Query("SELECT * FROM indexing_stats WHERE id = 'default' LIMIT 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Update
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)
}
