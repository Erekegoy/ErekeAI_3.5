package com.erekeai.data.remote.provider

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Role
import com.erekeai.domain.repository.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "Ollama Manager" (провайдерская часть) — реальный HTTP-клиент к Ollama (https://ollama.com),
 * которая крутится на компьютере пользователя (или другом устройстве в той же Wi-Fi сети) и
 * раздаёт локальные модели (llama3.2, qwen2.5, deepseek-r1 и т.д.). Это самый практичный способ
 * получить "локальный LLM" без компиляции нативного llama.cpp прямо на телефоне.
 *
 * Настройка: на ПК `ollama pull llama3.2` и `OLLAMA_HOST=0.0.0.0 ollama serve`, в Настройках
 * ErekeAI указать адрес, например http://192.168.1.50:11434, и имя модели.
 */
@Singleton
class OllamaProvider @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val httpClient: OkHttpClient
) : AiProvider {

    override val type = AiProviderType.OLLAMA

    @Volatile private var baseUrl: String = "http://127.0.0.1:11434"
    @Volatile private var model: String = "llama3.2"

    fun configure(baseUrl: String, model: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.model = model
    }

    override fun isConfigured(): Boolean = baseUrl.isNotBlank() && model.isNotBlank()

    override fun streamReply(history: List<ChatMessage>): Flow<String> = callbackFlow {
        val messages = JSONArray()
        history.forEach { msg ->
            if (msg.role == Role.TOOL) return@forEach
            messages.put(JSONObject().apply {
                put("role", when (msg.role) { Role.USER -> "user"; Role.ASSISTANT -> "assistant"; Role.SYSTEM -> "system"; Role.TOOL -> "user" })
                put("content", msg.text)
            })
        }
        val body = JSONObject().apply { put("model", model); put("messages", messages); put("stream", true) }.toString()
        val request = Request.Builder().url("$baseUrl/api/chat").post(body.toRequestBody("application/json".toMediaType())).build()

        val call = httpClient.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    trySend("Ошибка Ollama: HTTP ${response.code}. Проверьте, что 'ollama serve' запущен и доступен по $baseUrl")
                    close(); return@use
                }
                val source = response.body?.source() ?: run { close(); return@use }
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    try {
                        val obj = JSONObject(line)
                        val content = obj.optJSONObject("message")?.optString("content")
                        if (!content.isNullOrEmpty()) trySend(content)
                        if (obj.optBoolean("done", false)) break
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            trySend("Не удалось подключиться к Ollama по $baseUrl: ${e.message}")
        } finally { close() }
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    /** Список моделей, уже скачанных на Ollama-сервере (GET /api/tags) — для "Ollama Manager" UI/tool. */
    fun listModels(): List<String> {
        return try {
            val request = Request.Builder().url("$baseUrl/api/tags").build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val models = JSONObject(response.body?.string().orEmpty()).optJSONArray("models") ?: return emptyList()
                (0 until models.length()).map { i -> models.getJSONObject(i).optString("name") }
            }
        } catch (e: Exception) { emptyList() }
    }

    /** Запускает скачивание модели на сервере Ollama (POST /api/pull) — сервер тянет модель сам, телефон только инициирует. */
    fun pullModel(modelName: String): String {
        return try {
            val body = JSONObject().apply { put("name", modelName); put("stream", false) }.toString()
            val request = Request.Builder().url("$baseUrl/api/pull").post(body.toRequestBody("application/json".toMediaType())).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) "Загрузка модели '$modelName' запущена на сервере Ollama" else "Ошибка: HTTP ${response.code}"
            }
        } catch (e: Exception) { "Ошибка: ${e.message}" }
    }
}
