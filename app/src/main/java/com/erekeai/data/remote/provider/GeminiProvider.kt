package com.erekeai.data.remote.provider

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Role
import com.erekeai.domain.repository.AiProvider
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация провайдера для Google Gemini API (streamGenerateContent).
 */
@Singleton
class GeminiProvider @Inject constructor(
    private val keyStore: SecureKeyStore,
    httpClient: OkHttpClient
) : BaseHttpProvider(httpClient), AiProvider {

    override val type = AiProviderType.GEMINI

    override fun isConfigured(): Boolean = !keyStore.getKey(type.id).isNullOrBlank()

    override fun streamReply(history: List<ChatMessage>): Flow<String> {
        val apiKey = keyStore.getKey(type.id).orEmpty()
        val prompt = history.joinToString("\n") { "${it.role}: ${it.text}" }
        return streamRequest(prompt, apiKey)
    }

    override fun buildRequest(prompt: String, apiKey: String): Request {
        val contents = JSONArray().put(
            JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", prompt))
            )
        )
        val body = JSONObject().put("contents", contents).toString()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-1.5-flash:streamGenerateContent?alt=sse&key=$apiKey"
        return Request.Builder()
            .url(url)
            .post(jsonBody(body))
            .build()
    }

    override fun parseChunk(line: String): String? {
        if (!line.startsWith("data:")) return null
        return try {
            val json = JSONObject(line.removePrefix("data:").trim())
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        } catch (e: Exception) {
            null
        }
    }
}
