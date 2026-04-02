package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    //region DocumentEntity
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

    @Query("SELECT * FROM documents WHERE fileName LIKE '%' || :query || '%' OR contentPreview LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<DocumentEntity>
    //endregion

    //region DocumentContentEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Query("SELECT * FROM document_contents WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("DELETE FROM document_contents")
    suspend fun deleteAllDocumentContent()
    //endregion

    //region IndexingStatsEntity
    @Query("SELECT * FROM indexing_stats WHERE id = 'default' LIMIT 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Update
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndexingStats(stats: IndexingStatsEntity)
    //endregion

    //region SearchHistoryEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(history: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>
    //endregion

    //region MonitoredFolderEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredFolder(folder: MonitoredFolderEntity)

    @Query("SELECT * FROM monitored_folders WHERE isEnabled = 1")
    fun getEnabledMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    @Query("DELETE FROM monitored_folders WHERE id = :id")
    suspend fun deleteMonitoredFolder(id: Long)
    //endregion
}
