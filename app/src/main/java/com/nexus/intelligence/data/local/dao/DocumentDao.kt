package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entity.*

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentContent(content: DocumentContentEntity)

    @Update
    suspend fun updateDocumentContent(content: DocumentContentEntity)

    @Query("SELECT * FROM documents")
    suspend fun getAllDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContent(docId: Long): DocumentContentEntity?

    @Query("SELECT * FROM document_content WHERE documentId = :docId")
    suspend fun getDocumentContentWithEmbedding(docId: Long): DocumentContentEntity?

    @Query("SELECT id FROM documents")
    suspend fun getDocumentIdsWithEmbeddings(): List<Long>

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("DELETE FROM document_content")
    suspend fun deleteAllDocumentContent()
}
