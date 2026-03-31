package com.nexus.intelligence.data.repository

import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.DocumentContentEntity
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
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

    // Obtiene el contenido completo de un doc específico (solo cuando se abre)
    suspend fun getDocumentFullContent(docId: Long): String? = withContext(Dispatchers.IO) {
        documentDao.getDocumentContent(docId)?.fullTextContent
    }

    // ── Indexing ─────────────────────────────────────────────────────

    override suspend fun indexFile(file: File): DocumentInfo? = withContext(Dispatchers.IO) {
        try {
            val existing = documentDao.getDocumentByPath(file.absolutePath)
            if (existing != null && existing.lastModified >= file.lastModified()) {
                return@withContext existing.toDomainModel()
            }

            val parseResult = documentParser.parseFile(file)
            if (!parseResult.success) return@withContext null

            val contentPreview = parseResult.text.take(500)
            val typeLabel = DocumentParser.getDocumentTypeLabel(file)

            val entity = DocumentEntity(
                id = existing?.id ?: 0,
                filePath = file.absolutePath,
                fileName = file.name,
                fileType = typeLabel,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                indexedAt = System.currentTimeMillis(),
                contentPreview = contentPreview,
                parentDirectory = file.parent ?: "",
                mimeType = getMimeType(file),
                pageCount = parseResult.pageCount
            )

            val docId = documentDao.insertDocument(entity)

            // Guardar contenido pesado en tabla separada
            if (parseResult.text.isNotBlank()) {
                documentDao.insertDocumentContent(
                    DocumentContentEntity(
                        documentId = docId,
                        fullTextContent = parseResult.text
                    )
                )
            }

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
                // document_content se elimina en cascada por FK
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
            val relevance = calculateTextRelevance(query, entity)
            // Para el snippet jalamos solo el contenido de este doc específico
            val content = documentDao.getDocumentContent(entity.id)?.fullTextContent ?: entity.contentPreview
            SearchResult(
                document = entity.toDomainModel(),
                relevanceScore = relevance,
                matchedSnippet = extractSnippet(query, content),
                searchType = "TEXT"
            )
        }.sortedByDescending { it.relevanceScore }
    }

    override suspend fun semanticSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = embeddingService.getEmbedding(query) ?: return@withContext textSearch(query)

            val docIds = documentDao.getDocumentIdsWithEmbeddings()
            if (docIds.isEmpty()) return@withContext textSearch(query)

            // Carga embeddings uno por uno para no saturar memoria
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
            val textToEmbed = content?.fullTextContent?.take(1000)?.ifBlank { null }
                ?: doc.contentPreview.ifBlank { return@withContext false }

            val embedding = embeddingService.getEmbedding(textToEmbed) ?: return@withContext false
            val embeddingJson = embedding.joinToString(",", "[", "]")

            if (content != null) {
                documentDao.updateDocumentContent(content.copy(embeddingVector = embeddingJson))
            } else {
                documentDao.insertDocumentContent(
                    DocumentContentEntity(documentId = docId, embeddingVector = embeddingJson)
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Monitored Folders ────────────────────────────────────────────

    override fun getMonitoredFolders(): Flow<List<MonitoredFolderEntity>> {
        return documentDao.getAllMonitoredFolders()
    }

    override suspend fun addMonitoredFolder(path: String, label: String) {
        documentDao.insertMonitoredFolder(
            MonitoredFolderEntity(path = path, label = label)
        )
    }

    override suspend fun removeMonitoredFolder(path: String) {
        documentDao.deleteMonitoredFolderByPath(path)
    }

    // ── Search History ───────────────────────────────────────────────

    override suspend fun addSearchHistory(query: String, resultCount: Int) {
        documentDao.insertSearchHistory(
            SearchHistoryEntity(query = query, resultCount = resultCount)
        )
    }

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> {
        return documentDao.getRecentSearches()
    }

    // ── Stats ────────────────────────────────────────────────────────

    override fun getIndexingStats(): Flow<IndexingStatsEntity?> {
        return documentDao.getIndexingStats()
    }

    override suspend fun updateIndexingStats(stats: IndexingStatsEntity) {
        documentDao.updateIndexingStats(stats)
    }

    // ── API Status ───────────────────────────────────────────────────

    override suspend fun isApiAvailable(): Boolean = embeddingService.isApiAvailable()

    // ── Private Helpers ──────────────────────────────────────────────

    private fun calculateTextRelevance(query: String, entity: DocumentEntity): Float {
        val queryLower = query.lowercase()
        val nameLower = entity.fileName.lowercase()
        val previewLower = entity.contentPreview.lowercase()

        var score = 0f
        if (nameLower.contains(queryLower)) score += 0.4f

        val words = queryLower.split(" ").filter { it.length > 2 }
        for (word in words) {
            val count = previewLower.windowed(word.length) { it }.count { it == word }
            score += (count.coerceAtMost(10) * 0.05f)
        }

        return score.coerceIn(0f, 1f)
    }

    private fun extractSnippet(query: String, content: String, snippetLength: Int = 200): String {
        val queryLower = query.lowercase()
        val contentLower = content.lowercase()
        val index = contentLower.indexOf(queryLower)

        return if (index >= 0) {
            val start = (index - snippetLength / 2).coerceAtLeast(0)
            val end = (start + snippetLength).coerceAtMost(content.length)
            "…${content.substring(start, end)}…"
        } else {
            content.take(snippetLength) + "…"
        }
    }

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

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}

// ── Extension Functions ──────────────────────────────────────────

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
        hasEmbedding = false // Se actualiza al consultar document_content
    )
}
