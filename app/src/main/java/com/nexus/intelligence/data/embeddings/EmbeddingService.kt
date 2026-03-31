package com.nexus.intelligence.data.embeddings

import android.content.Context
import com.google.gson.Gson
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

data class EmbeddingRequest(
    val model: String = "nomic-embed-text",
    val input: List<String>
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>?
)

data class EmbeddingData(
    val embedding: List<Float>
)

@Singleton
class EmbeddingService @Inject constructor(
    private val context: Context
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getEmbedding(text: String): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val requestBody = gson.toJson(
                EmbeddingRequest(input = listOf(text))
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://127.0.0.1:11434/api/embeddings")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val parsed = gson.fromJson(body, EmbeddingResponse::class.java)
            val embedding = parsed.data?.firstOrNull()?.embedding ?: return@withContext null

            embedding.toFloatArray()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isApiAvailable(): Boolean {
        return try {
            getEmbedding("test") != null
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            return dot / (sqrt(normA) * sqrt(normB))
        }

        fun findTopK(
            query: FloatArray,
            docs: List<Pair<Long, FloatArray>>,
            k: Int
        ): List<Pair<Long, Float>> {
            return docs.map { (id, emb) ->
                id to cosineSimilarity(query, emb)
            }.sortedByDescending { it.second }
                .take(k)
        }
    }
}
