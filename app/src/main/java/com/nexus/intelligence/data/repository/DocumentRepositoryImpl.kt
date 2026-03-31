package com.nexus.intelligence.data.repository

import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.*
import com.nexus.intelligence.data.parser.DocumentParser
import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val documentParser: DocumentParser,
    private val embeddingService: EmbeddingService
) : DocumentRepository {

    // ── Document CRUD ────────────────────────────────────────────────

    override fun getAllDocuments(): Flow<List<DocumentInfo>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecentDocuments(limit: Int): Flow<List<DocumentInfo>> {
        return documentDao.getRecentDocuments(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentsByType(type: String): Flow<List<DocumentInfo>> {
        return documentDao.getDocumentsByType(type).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentsByDirectory(directory: String): Flow<List<DocumentInfo>> {
        return documentDao.getDocumentsByDirectory(directory).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentCount(): Flow<Int> = documentDao.getDocumentCount()

    override fun getAllDirectories(): Flow<List<String>> = documentDao.getAllDirectories()

    override suspend fun getDocumentById(id: Long): DocumentInfo? {
        return documentDao.getDocumentById(id)?.toDomainModel()
    }

    // ── Indexing ─────────────────────────────────────────────────────

    override suspend fun indexFile(file: File): DocumentInfo? = withContext(Dispatchers.IO) {
        try {
            val parseResult = documentParser.parseFile(file)
            if (!parseResult.success) return@withContext null

            val entity = DocumentEntity(
                filePath = file.absolutePath,
                fileName = file.name,
                fileType = DocumentParser.getDocumentTypeLabel(file),
                fileSize = file.length(),
                lastModified = file.lastModified(),
                indexedAt = System.currentTimeMillis(),
                contentPreview = parseResult.text.take(500),
                parentDirectory = file.parent ?: "",
                mimeType = "unknown",
                pageCount = parseResult.pageCount
            )

            val docId = documentDao.insertDocument(entity)

            if (parseResult.text.isNotBlank()) {
                documentDao.insertDocumentContent(
                    DocumentContentEntity(
                        documentId = docId,
                        fullTextContent = parseResult.text
                    )
                )
            }

            generateEmbeddingsForDocument(docId)

            entity.copy(id = docId).toDomainModel()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun removeDeletedFiles() = withContext(Dispatchers.IO) {
        val allPaths = documentDao.getAllFilePaths()
        for (path in allPaths) {
            if (!File(path).exists()) {
                documentDao.deleteByPath(path)
            }
        }
    }

    override suspend fun clearIndex() {
        documentDao.deleteAllDocuments()
        documentDao.deleteAllDocumentContent()
    }

    // ── Search ───────────────────────────────────────────────────────

    override suspend fun textSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = documentDao.searchDocuments(query)
        results.map { entity ->
            val content = documentDao.getDocumentContent(entity.id)?.fullTextContent ?: entity.contentPreview
            SearchResult(
                document = entity.toDomainModel(),
                relevanceScore = 1.0f, // Simplificado para mantener consistencia con los cambios del usuario
                matchedSnippet = content.take(200),
                searchType = "TEXT"
            )
        }
    }

    override suspend fun semanticSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = embeddingService.getEmbedding(query) ?: return@withContext textSearch(query)

            val docIds = documentDao.getDocumentIdsWithEmbeddings()
            if (docIds.isEmpty()) return@withContext textSearch(query)

            val docEmbeddings = docIds.mapNotNull { docId ->
                val content = documentDao.getDocumentContentWithEmbedding(docId) ?: return@mapNotNull null
                val embedding = parseEmbeddingVector(content.embeddingVector) ?: return@mapNotNull null
                docId to embedding
            }

            val topMatches = EmbeddingService.findTopK(queryEmbedding, docEmbeddings, 20)

            topMatches.mapNotNull { (docId, similarity) ->
                val doc = documentDao.getDocumentById(docId) ?: return@mapNotNull null
                val preview = documentDao.getDocumentContent(docId)?.fullTextContent?.take(200)
                    ?: doc.contentPreview
                SearchResult(
                    document = doc.toDomainModel(),
                    relevanceScore = similarity,
                    matchedSnippet = preview,
                    searchType = "SEMANTIC"
                )
            }
        } catch (e: Exception) {
            textSearch(query)
        }
    }

    override suspend fun generateEmbeddingsForDocument(docId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = documentDao.getDocumentById(docId) ?: return@withContext false
            val content = documentDao.getDocumentContent(docId)

            val text = content?.fullTextContent?.take(1000)
                ?: doc.contentPreview

            val embedding = embeddingService.getEmbedding(text) ?: return@withContext false
            val json = embedding.joinToString(",", "[", "]")

            if (content != null) {
                documentDao.updateDocumentContent(
                    content.copy(embeddingVector = json)
                )
            } else {
                documentDao.insertDocumentContent(
                    DocumentContentEntity(
                        documentId = docId,
                        embeddingVector = json
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Monitored Folders ────────────────────────────────────────────

    override fun getMonitoredFolders(): Flow<List<MonitoredFolderEntity>> = documentDao.getAllMonitoredFolders()

    override suspend fun addMonitoredFolder(path: String, label: String) {
        documentDao.insertMonitoredFolder(MonitoredFolderEntity(path = path, label = label))
    }

    override suspend fun removeMonitoredFolder(path: String) {
        documentDao.deleteMonitoredFolderByPath(path)
    }

    // ── Search History ───────────────────────────────────────────────

    override suspend fun addSearchHistory(query: String, resultCount: Int) {
        documentDao.insertSearchHistory(SearchHistoryEntity(query = query, resultCount = resultCount))
    }

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = documentDao.getRecentSearches()

    // ── Stats ────────────────────────────────────────────────────────

    override fun getIndexingStats(): Flow<IndexingStatsEntity?> = documentDao.getIndexingStats()

    override suspend fun updateIndexingStats(stats: IndexingStatsEntity) {
        documentDao.updateIndexingStats(stats)
    }

    // ── API Status ───────────────────────────────────────────────────

    override suspend fun isApiAvailable(): Boolean = embeddingService.isApiAvailable()

    // ── Helpers ──────────────────────────────────────────────────────

    private fun parseEmbeddingVector(json: String?): FloatArray? {
        if (json == null) return null
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            null
        }
    }
}

fun DocumentEntity.toDomainModel(): DocumentInfo {
    return DocumentInfo(
        id = id,
        filePath = filePath,
        fileName = fileName,
        fileType = fileType,
        fileSize = fileSize,
        lastModified = lastModified,
        indexedAt = indexedAt,
        contentPreview = contentPreview,
        parentDirectory = parentDirectory,
        mimeType = mimeType,
        pageCount = pageCount,
        isFromNetwork = isFromNetwork,
        networkSourceDevice = networkSourceDevice,
        hasEmbedding = false
    )
}
