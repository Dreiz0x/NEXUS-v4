package com.nexus.intelligence.data.embeddings

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

// ── API Request/Response Models ──────────────────────────────────

data class EmbeddingRequest(
    val model: String = "default",
    val input: List<String>
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>?,
    val model: String?,
    val usage: UsageInfo?
)

data class EmbeddingData(
    val embedding: List<Float>,
    val index: Int
)

data class UsageInfo(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

data class ChatRequest(
    val model: String = "default",
    val messages: List<ChatMessage>,
    val temperature: Float = 0.1f,
    @SerializedName("max_tokens") val maxTokens: Int = 500
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    val message: ChatMessage?
)

// ── Embedding Service ────────────────────────────────────────────

@Singleton
class EmbeddingService @Inject constructor(
    context: Context  // Agregado para leer prefs al init
) {
    private val gson = Gson()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)

    // Lee la URL guardada por el usuario desde el primer momento
    private var baseUrl: String = prefs.getString("api_endpoint", "http://127.0.0.1:8080")
        ?: "http://127.0.0.1:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)  // Reducido — falla rápido si no hay servidor
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
        // También persiste el cambio
        prefs.edit().putString("api_endpoint", baseUrl).apply()
    }

    fun getBaseUrl(): String = baseUrl

    /**
     * Checa si el servidor local está disponible.
     * Compatible con llama.cpp, Ollama, LM Studio, Jan, y cualquier servidor
     * OpenAI-compatible — intenta varios endpoints comunes.
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        val endpointsToTry = listOf(
            "$baseUrl/health",          // llama.cpp
            "$baseUrl/v1/models",       // OpenAI-compatible estándar
            "$baseUrl/api/tags",        // Ollama
            "$baseUrl/"                 // Fallback genérico
        )

        for (endpoint in endpointsToTry) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                // Cualquier respuesta HTTP (incluso 404) significa que el servidor está vivo
                if (response.code < 500) return@withContext true
            } catch (e: Exception) {
                continue  // Intenta el siguiente endpoint
            }
        }
        false
    }

    /**
     * Genera embeddings. Compatible con servidores OpenAI-compatible locales.
     */
    suspend fun getEmbeddings(texts: List<String>): List<FloatArray>? = withContext(Dispatchers.IO) {
        try {
            val requestBody = EmbeddingRequest(input = texts)
            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/v1/embeddings")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val embeddingResponse = gson.fromJson(body, EmbeddingResponse::class.java)

            embeddingResponse.data?.sortedBy { it.index }?.map { it.embedding.toFloatArray() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEmbedding(text: String): FloatArray? {
        return getEmbeddings(listOf(text))?.firstOrNull()
    }

    /**
     * Chat completion — compatible con llama.cpp /v1/chat/completions y Ollama.
     */
    suspend fun semanticSearch(
        query: String,
        documentContexts: List<Pair<String, String>>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contextText = documentContexts.joinToString("\n\n") { (name, content) ->
                "=== Document: $name ===\n$content"
            }

            val messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are NEXUS, a document intelligence assistant. Given the following document excerpts, answer the user's query. Reference specific documents by name. Be concise and precise."
                ),
                ChatMessage(
                    role = "user",
                    content = "Documents:\n$contextText\n\nQuery: $query"
                )
            )

            val requestBody = ChatRequest(messages = messages)
            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val chatResponse = gson.fromJson(body, ChatResponse::class.java)
            chatResponse.choices?.firstOrNull()?.message?.content
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dotProduct = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dotProduct += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0f) 0f else dotProduct / denominator
        }

        fun findTopK(
            queryEmbedding: FloatArray,
            documentEmbeddings: List<Pair<Long, FloatArray>>,
            topK: Int = 10
        ): List<Pair<Long, Float>> {
            return documentEmbeddings
                .map { (id, emb) -> id to cosineSimilarity(queryEmbedding, emb) }
                .sortedByDescending { it.second }
                .take(topK)
        }
    }
}
