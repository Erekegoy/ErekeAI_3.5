package com.erekeai.data.embedding

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.vector.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реальные эмбеддинги через OpenAI API (text-embedding-3-small — компактная,
 * недорогая модель, 1536 измерений по умолчанию, можно урезать через dimensions).
 */
@Singleton
class OpenAiEmbeddingProvider @Inject constructor(
    private val keyStore: SecureKeyStore,
    private val client: OkHttpClient
) : EmbeddingProvider {

    override val id = "openai"
    override val dimensions = 768

    override fun isConfigured(): Boolean = !keyStore.getKey("openai").isNullOrBlank()

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val apiKey = keyStore.getKey("openai")
            ?: throw IllegalStateException("OpenAI API-ключ не задан")

        val body = JSONObject().apply {
            put("model", "text-embedding-3-small")
            put("input", text.take(8000))
            put("dimensions", dimensions)
        }.toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/embeddings")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("OpenAI embeddings HTTP ${response.code}: $responseBody")
            }
            val values = JSONObject(responseBody)
                .getJSONArray("data")
                .getJSONObject(0)
                .getJSONArray("embedding")
            FloatArray(values.length()) { i -> values.getDouble(i).toFloat() }
        }
    }
}
