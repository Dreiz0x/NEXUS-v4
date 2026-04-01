package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entity.DocumentContentEntity
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Update
    suspend fun updateDocumentContent(content: DocumentContentEntity)

    // Fíjate que devolvemos Flow y quitamos 'suspend'
    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

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

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContentWithEmbedding(docId: Long): DocumentContentEntity?

    @Query("SELECT id FROM documents WHERE contentPreview IS NOT NULL") // Ajuste lógico
    suspend fun getDocumentIdsWithEmbeddings(): List<Long>

    @Query("SELECT filePath FROM documents")
    suspend fun getAllFilePaths(): List<String>

    @Query("DELETE FROM documents WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("DELETE FROM document_content")
    suspend fun deleteAllDocumentContent()

    @Query("SELECT * FROM documents WHERE fileName LIKE '%' || :query || '%' OR contentPreview LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    // ── Monitored Folders ──
    @Query("SELECT * FROM monitored_folders")
    fun getAllMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredFolder(folder: MonitoredFolderEntity)

    @Query("DELETE FROM monitored_folders WHERE path = :path")
    suspend fun deleteMonitoredFolderByPath(path: String)

    // ── Search History ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(history: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    // ── Stats ──
    @Query("SELECT * FROM indexing_stats LIMIT 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Update
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)
}
