package com.nexus.intelligence.domain.usecase

import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.embeddings.ChatMessage
import com.nexus.intelligence.data.embeddings.ChatRequest
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.FileCluster
import com.nexus.intelligence.domain.model.GenericFileInfo
import com.nexus.intelligence.domain.repository.DocumentRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FileOrganizerUseCase @Inject constructor(
    private val repository: DocumentRepository,
    private val embeddingService: EmbeddingService,
    private val documentDao: DocumentDao
) {
    private val _progress = MutableStateFlow("")
    val progress: Flow<String> = _progress.asStateFlow()

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // ── Análisis principal ───────────────────────────────────────────

    suspend fun analyzeDocuments(documents: List<DocumentInfo>): List<FileCluster> {
        if (documents.isEmpty()) return emptyList()

        _progress.value = "Detectando archivos con nombres genéricos..."
        val genericInfos = documents.map { doc ->
            GenericFileInfo(
                document = doc,
                isGenericName = isGenericName(doc.fileName)
            )
        }

        _progress.value = "Generando embeddings para clustering..."
        val embeddings = generateEmbeddingsForAll(documents)

        _progress.value = "Agrupando documentos por similitud..."
        val rawClusters = clusterBySimilarity(documents, embeddings)

        _progress.value = "Consultando modelo local para nombres de carpetas..."
        val namedClusters = nameClusterswithLLM(rawClusters)

        _progress.value = "Listo"
        return namedClusters
    }

    // ── Detección de nombres genéricos ───────────────────────────────

    fun isGenericName(fileName: String): Boolean {
        val name = fileName.substringBeforeLast(".").lowercase().trim()
        if (name.length < 2) return true

        // Sin vocales = probable hash/basura
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        val hasVowels = name.any { it in vowels }
        if (!hasVowels && name.length > 3) return true

        // Patrones genéricos conocidos
        val genericPatterns = listOf(
            Regex("^[0-9a-f]{6,}$"),                    // hex puro
            Regex("^\\d+$"),                             // solo números
            Regex("^(doc|file|document|archivo|descarga|download|untitled|unnamed)\\d*$"),
            Regex("^(img|image|foto|photo|pic|screenshot|captura)\\d*$"),
            Regex("^[a-z]{1,3}\\d{4,}$"),               // letras + timestamp
            Regex("^\\d{8,}$"),                          // timestamp numérico
            Regex("^[a-z0-9]{8,}$").also { }            // cadena alfanumérica larga sin sentido
        )

        // Ratio consonante/vocal muy alto = probable basura
        val letters = name.filter { it.isLetter() }
        if (letters.length > 4) {
            val consonantRatio = letters.count { it !in vowels }.toFloat() / letters.length
            if (consonantRatio > 0.85f) return true
        }

        return genericPatterns.any { it.matches(name) }
    }

    // ── Generación de embeddings ─────────────────────────────────────

    private suspend fun generateEmbeddingsForAll(
        documents: List<DocumentInfo>
    ): Map<Long, FloatArray> {
        val result = mutableMapOf<Long, FloatArray>()
        val batchSize = 10

        documents.chunked(batchSize).forEachIndexed { batchIdx, batch ->
            _progress.value = "Embeddings: ${batchIdx * batchSize}/${documents.size}..."

            // Intentar usar embeddings ya guardados en DB
            val needsEmbedding = mutableListOf<DocumentInfo>()
            for (doc in batch) {
                val existing = documentDao.getDocumentContentWithEmbedding(doc.id)
                if (existing?.embeddingVector != null) {
                    parseVector(existing.embeddingVector)?.let { result[doc.id] = it }
                } else {
                    needsEmbedding.add(doc)
                }
            }

            if (needsEmbedding.isEmpty()) return@forEachIndexed

            // Generar embeddings para los que no tienen
            val texts = needsEmbedding.map { doc ->
                "${doc.fileName} ${doc.contentPreview}".take(512)
            }

            val embeddings = embeddingService.getEmbeddings(texts) ?: return@forEachIndexed

            needsEmbedding.forEachIndexed { i, doc ->
                embeddings.getOrNull(i)?.let { emb ->
                    result[doc.id] = emb
                }
            }
        }

        return result
    }

    // ── Clustering por similitud coseno ──────────────────────────────

    private fun clusterBySimilarity(
        documents: List<DocumentInfo>,
        embeddings: Map<Long, FloatArray>,
        threshold: Float = 0.55f
    ): List<List<DocumentInfo>> {
        if (embeddings.isEmpty()) {
            // Fallback: un solo cluster con todos
            return listOf(documents)
        }

        val assigned = mutableSetOf<Long>()
        val clusters = mutableListOf<MutableList<DocumentInfo>>()

        for (doc in documents) {
            if (doc.id in assigned) continue

            val cluster = mutableListOf(doc)
            assigned.add(doc.id)

            val docEmb = embeddings[doc.id] ?: continue

            for (other in documents) {
                if (other.id in assigned) continue
                val otherEmb = embeddings[other.id] ?: continue

                val similarity = cosineSimilarity(docEmb, otherEmb)
                if (similarity >= threshold) {
                    cluster.add(other)
                    assigned.add(other.id)
                }
            }

            clusters.add(cluster)
        }

        // Documentos sin embedding van a un cluster "Sin clasificar"
        val unassigned = documents.filter { it.id !in assigned }
        if (unassigned.isNotEmpty()) clusters.add(unassigned.toMutableList())

        // Filtrar clusters de 1 solo archivo (no tiene sentido moverlos solos)
        // Los de 1 archivo se agrupan en "Varios"
        val singletons = clusters.filter { it.size == 1 }.flatten()
        val multiClusters = clusters.filter { it.size > 1 }.toMutableList()
        if (singletons.isNotEmpty()) multiClusters.add(singletons.toMutableList())

        return multiClusters
    }

    // ── Naming con LLM local ─────────────────────────────────────────

    private suspend fun nameClusterswithLLM(
        rawClusters: List<List<DocumentInfo>>
    ): List<FileCluster> {
        return rawClusters.mapIndexed { idx, docs ->
            _progress.value = "Nombrando grupo ${idx + 1}/${rawClusters.size}..."

            val suggestedName = askLLMForFolderName(docs) ?: generateFallbackName(docs, idx)

            FileCluster(
                id = UUID.randomUUID().toString(),
                suggestedFolderName = sanitizeFolderName(suggestedName),
                documents = docs
            )
        }
    }

    private suspend fun askLLMForFolderName(docs: List<DocumentInfo>): String? {
        return try {
            val fileList = docs.take(8).joinToString("\n") { doc ->
                "- ${doc.fileName}: ${doc.contentPreview.take(100)}"
            }

            val prompt = """Analiza estos archivos y sugiere UN nombre de carpeta corto (2-4 palabras en español) que los agrupe temáticamente. 
Solo responde con el nombre de la carpeta, sin explicaciones ni puntuación extra.

Archivos:
$fileList

Nombre de carpeta:"""

            val request = ChatRequest(
                messages = listOf(
                    ChatMessage(role = "system", content = "Eres un asistente de organización de archivos. Solo respondes con nombres de carpetas cortos y descriptivos en español."),
                    ChatMessage(role = "user", content = prompt)
                ),
                maxTokens = 20,
                temperature = 0.3f
            )

            val json = gson.toJson(request)
            val httpRequest = Request.Builder()
                .url("${embeddingService.getBaseUrl()}/v1/chat/completions")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val chatResponse = gson.fromJson(body, com.nexus.intelligence.data.embeddings.ChatResponse::class.java)
            chatResponse.choices?.firstOrNull()?.message?.content?.trim()
                ?.take(50)
                ?.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _\\-]"), "")
                ?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun generateFallbackName(docs: List<DocumentInfo>, idx: Int): String {
        // Extrae la palabra más común en los nombres de archivo
        val words = docs.flatMap { doc ->
            doc.fileName.substringBeforeLast(".")
                .replace(Regex("[_\\-.]"), " ")
                .split(" ")
                .filter { it.length > 3 && !isGenericName("$it.x") }
        }

        val mostCommon = words.groupingBy { it.lowercase() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return mostCommon?.replaceFirstChar { it.uppercase() } ?: "Grupo ${idx + 1}"
    }

    private fun sanitizeFolderName(name: String): String {
        return name
            .replace(Regex("[/\\\\:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(50)
            .ifEmpty { "Carpeta sin nombre" }
    }

    private fun parseVector(json: String?): FloatArray? {
        if (json == null) return null
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) { null }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }
}
