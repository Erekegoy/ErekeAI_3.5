package com.erekeai.data.remote.provider

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.repository.AiProvider
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Провайдер Groq (быстрый инференс open-source моделей, API совместим с форматом OpenAI Chat Completions).
 */
@Singleton
class GroqProvider @Inject constructor(
    private val keyStore: SecureKeyStore,
    httpClient: OkHttpClient
) : BaseHttpProvider(httpClient), AiProvider {

    override val type = AiProviderType.GROQ

    override fun isConfigured(): Boolean = !keyStore.getKey(type.id).isNullOrBlank()

    override fun streamReply(history: List<ChatMessage>): Flow<String> {
        val apiKey = keyStore.getKey(type.id).orEmpty()
        val prompt = history.lastOrNull()?.text.orEmpty()
        return streamRequest(prompt, apiKey)
    }

    override fun buildRequest(prompt: String, apiKey: String): Request {
        val messages = JSONArray().put(
            JSONObject().put("role", "user").put("content", prompt)
        )
        val body = JSONObject()
            .put("model", "llama-3.3-70b-versatile")
            .put("messages", messages)
            .put("stream", true)
            .toString()
        return Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody(body))
            .build()
    }

    override fun parseChunk(line: String): String? {
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload == "[DONE]") return null
        return try {
            JSONObject(payload)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("delta")
                ?.optString("content")
        } catch (e: Exception) {
            null
        }
    }
}
