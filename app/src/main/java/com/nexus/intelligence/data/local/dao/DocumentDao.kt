package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entity.DocumentContentEntity
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // ── Document Operations ──────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    // Sin SELECT * — nunca jala fullTextContent ni embeddingVector en listados
    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents ORDER BY indexedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents ORDER BY indexedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 50): Flow<List<DocumentEntity>>

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents WHERE filePath = :path")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents WHERE fileType = :type ORDER BY lastModified DESC")
    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>>

    // Búsqueda: usa JOIN con document_content para no traer todo el texto
    @Query("""
        SELECT d.id, d.filePath, d.fileName, d.fileType, d.fileSize, d.lastModified, 
               d.indexedAt, d.contentPreview, d.parentDirectory, d.mimeType, 
               d.pageCount, d.isFromNetwork, d.networkSourceDevice
        FROM documents d
        LEFT JOIN document_content dc ON d.id = dc.documentId
        WHERE d.fileName LIKE '%' || :query || '%'
           OR dc.fullTextContent LIKE '%' || :query || '%'
        ORDER BY d.lastModified DESC
    """)
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents WHERE parentDirectory = :directory ORDER BY fileName ASC")
    fun getDocumentsByDirectory(directory: String): Flow<List<DocumentEntity>>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE fileType = :type")
    suspend fun getCountByType(type: String): Int

    @Query("SELECT DISTINCT parentDirectory FROM documents ORDER BY parentDirectory ASC")
    fun getAllDirectories(): Flow<List<String>>

    @Query("SELECT filePath FROM documents")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT id, filePath, fileName, fileType, fileSize, lastModified, indexedAt, contentPreview, parentDirectory, mimeType, pageCount, isFromNetwork, networkSourceDevice FROM documents WHERE lastModified > :since ORDER BY lastModified DESC")
    suspend fun getDocumentsModifiedSince(since: Long): List<DocumentEntity>

    // ── Document Content (tabla separada para blobs) ──────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("DELETE FROM document_content WHERE documentId = :docId")
    suspend fun deleteDocumentContent(docId: Long)

    @Query("DELETE FROM document_content")
    suspend fun deleteAllDocumentContent()

    // Solo IDs de docs que tienen embedding — no trae el vector en este query
    @Query("SELECT documentId FROM document_content WHERE embeddingVector IS NOT NULL")
    suspend fun getDocumentIdsWithEmbeddings(): List<Long>

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContentWithEmbedding(docId: Long): DocumentContentEntity?

    @Update
    suspend fun updateDocumentContent(content: DocumentContentEntity)

    // ── Monitored Folders ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredFolder(folder: MonitoredFolderEntity): Long

    @Delete
    suspend fun deleteMonitoredFolder(folder: MonitoredFolderEntity)

    @Query("DELETE FROM monitored_folders WHERE path = :path")
    suspend fun deleteMonitoredFolderByPath(path: String)

    @Query("SELECT * FROM monitored_folders WHERE isEnabled = 1 ORDER BY addedAt DESC")
    fun getActiveMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    @Query("SELECT * FROM monitored_folders ORDER BY addedAt DESC")
    fun getAllMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    // ── Search History ───────────────────────────────────────────────

    @Insert
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // ── Indexing Stats ───────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)

    @Query("SELECT * FROM indexing_stats WHERE id = 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Query("SELECT * FROM indexing_stats WHERE id = 1")
    suspend fun getIndexingStatsOnce(): IndexingStatsEntity?
}
