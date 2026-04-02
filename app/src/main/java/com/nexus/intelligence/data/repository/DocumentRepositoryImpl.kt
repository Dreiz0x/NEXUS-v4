package com.nexus.intelligence.data.repository

import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.*
import com.nexus.intelligence.data.parser.DocumentParser
import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

    private val _isScanning = MutableStateFlow(false)
    override val isCurrentlyScanning: Flow<Boolean> = _isScanning

    private val _lastScan = MutableStateFlow(System.currentTimeMillis())
    override val lastScanTimestamp: Flow<Long> = _lastScan

    override fun getAllDocuments(): Flow<List<DocumentInfo>> =
        documentDao.getAllDocuments().map { list -> list.map { it.toDomainModel() } }

    override fun getRecentDocuments(limit: Int): Flow<List<DocumentInfo>> =
        documentDao.getRecentDocuments(limit).map { list -> list.map { it.toDomainModel() } }

    override fun getDocumentsByType(type: String): Flow<List<DocumentInfo>> =
        documentDao.getDocumentsByType(type).map { list -> list.map { it.toDomainModel() } }

    override fun getDocumentsByDirectory(directory: String): Flow<List<DocumentInfo>> =
        documentDao.getDocumentsByDirectory(directory).map { list -> list.map { it.toDomainModel() } }

    override fun getDocumentCount(): Flow<Int> = documentDao.getDocumentCount()

    override fun getAllDirectories(): Flow<List<String>> = documentDao.getAllDirectories()

    override suspend fun getDocumentById(id: Long): DocumentInfo? =
        documentDao.getDocumentById(id)?.toDomainModel()

    override suspend fun getDocumentByPath(path: String): DocumentInfo? =
        documentDao.getDocumentByPath(path)?.toDomainModel()

    override suspend fun updateDocument(document: DocumentInfo) {
        // Puedes mapear a Entity si luego necesitas update real
    }

    override suspend fun getAllFilePaths(): List<String> =
        documentDao.getAllFilePaths()

    override suspend fun deleteByPath(path: String) =
        documentDao.deleteByPath(path)

    override suspend fun indexFile(file: File): DocumentInfo? = withContext(Dispatchers.IO) {
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
            mimeType = "application/pdf",
            pageCount = parseResult.pageCount
        )

        val id = documentDao.insertDocument(entity)

        documentDao.insertDocumentContent(
            DocumentContentEntity(
                documentId = id,
                fullTextContent = parseResult.text
            )
        )

        entity.copy(id = id).toDomainModel()
    }

    override suspend fun removeDeletedFiles() {
        val paths = documentDao.getAllFilePaths()
        paths.forEach {
            if (!File(it).exists()) {
                documentDao.deleteByPath(it)
            }
        }
    }

    override suspend fun clearIndex() {
        documentDao.deleteAllDocuments()
        documentDao.deleteAllDocumentContent()
    }

    override suspend fun textSearch(query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            documentDao.searchDocuments(query).map {
                SearchResult(
                    document = it.toDomainModel(),
                    score = 1.0f,
                    snippet = it.contentPreview,
                    source = "TEXT"
                )
            }
        }

    override suspend fun semanticSearch(query: String): List<SearchResult> =
        textSearch(query)

    override suspend fun generateEmbeddingsForDocument(docId: Long): Boolean = true

    // =========================
    // ✅ MONITORED FOLDERS FIX
    // =========================

    override fun getMonitoredFolders(): Flow<List<MonitoredFolderEntity>> =
        documentDao.getAllMonitoredFolders()

    override suspend fun addMonitoredFolder(path: String, label: String) {
        documentDao.insertMonitoredFolder(
            MonitoredFolderEntity(
                path = path,
                label = label
            )
        )
    }

    override suspend fun removeMonitoredFolder(path: String) =
        documentDao.deleteMonitoredFolderByPath(path)

    // =========================
    // HISTORIAL / STATS
    // =========================

    override suspend fun addSearchHistory(query: String, resultCount: Int) {
        documentDao.insertSearchHistory(
            SearchHistoryEntity(
                query = query,
                resultCount = resultCount
            )
        )
    }

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> =
        documentDao.getRecentSearches()

    override fun getIndexingStats(): Flow<IndexingStatsEntity?> =
        documentDao.getIndexingStats()

    override suspend fun updateIndexingStats(stats: IndexingStatsEntity) =
        documentDao.updateIndexingStats(stats)

    // =========================
    // CONTROL
    // =========================

    override fun startScan() {
        _isScanning.value = true
    }

    override fun stop() {
        _isScanning.value = false
    }

    override suspend fun isApiAvailable(): Boolean =
        embeddingService.isApiAvailable()
}

// =========================
// MAPPER
// =========================

fun DocumentEntity.toDomainModel() = DocumentInfo(
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
    isFavorite = false
)
