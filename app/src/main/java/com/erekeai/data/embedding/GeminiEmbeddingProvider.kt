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
 * Реальные эмбеддинги через Gemini API (gemini-embedding-001, actуальная
 * стабильная модель на момент написания — embedContent эндпоинт).
 */
@Singleton
class GeminiEmbeddingProvider @Inject constructor(
    private val keyStore: SecureKeyStore,
    private val client: OkHttpClient
) : EmbeddingProvider {

    override val id = "gemini"
    override val dimensions = 768 // запрашиваем усечённую (Matryoshka) размерность — компактнее в БД

    override fun isConfigured(): Boolean = !keyStore.getKey("gemini").isNullOrBlank()

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val apiKey = keyStore.getKey("gemini")
            ?: throw IllegalStateException("Gemini API-ключ не задан")

        val body = JSONObject().apply {
            put("model", "models/gemini-embedding-001")
            put("content", JSONObject().put(
                "parts", org.json.JSONArray().put(JSONObject().put("text", text.take(8000)))
            ))
            put("outputDimensionality", dimensions)
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini embeddings HTTP ${response.code}: $responseBody")
            }
            val values = JSONObject(responseBody)
                .getJSONObject("embedding")
                .getJSONArray("values")
            FloatArray(values.length()) { i -> values.getDouble(i).toFloat() }
        }
    }
}
