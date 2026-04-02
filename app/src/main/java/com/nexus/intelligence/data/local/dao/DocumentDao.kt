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

    // Contenido (Usando la tabla correcta: document_contents)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Query("SELECT * FROM document_contents WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("DELETE FROM document_contents")
    suspend fun deleteAllDocumentContent()

    // Stats y otros
    @Query("SELECT * FROM indexing_stats WHERE id = 'default' LIMIT 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Update
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(history: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>
}
