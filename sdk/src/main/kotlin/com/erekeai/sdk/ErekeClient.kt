package com.erekeai.sdk

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ToolExecutionResult(val success: Boolean, val content: String)

/**
 * 🟡 Готовый клиент для использования ErekeAI как "ядра" из ваших других проектов.
 * Пример использования из другого Android-приложения или JVM-скрипта:
 *
 * ```kotlin
 * val ereke = ErekeClient(baseUrl = "http://192.168.1.20:8765")
 * val reply = ereke.runAgent("проверь погоду и запомни, что мне нужно взять зонт")
 * println(reply)
 * ```
 *
 * Требует, чтобы на телефоне с ErekeAI был включён REST API-сервер (Настройки → API/SDK)
 * и оба устройства находились в одной сети (или сервер проброшен наружу вами самостоятельно).
 */
class ErekeClient(
    private val baseUrl: String,
    private val authToken: String? = null,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // агент может выполнять несколько шагов — даём запас
        .build()
) {
    fun isHealthy(): Boolean = try {
        get("/health").optString("status") == "ok"
    } catch (e: Exception) { false }

    /** Запускает полноценный AI Agent (ReAct-цикл с инструментами) и возвращает финальный текстовый ответ. */
    fun runAgent(text: String, provider: String = "gemini", persona: String = "general", conversationId: Long = 0L): String {
        val body = JSONObject().apply {
            put("text", text); put("provider", provider); put("persona", persona); put("conversationId", conversationId)
        }
        return post("/agent/run", body).optString("reply")
    }

    /** Список доступных инструментов ErekeAI. */
    fun listTools(): List<String> {
        val request = buildRequest("/tools/list").get().build()
        httpClient.newCall(request).execute().use { response ->
            val arr = org.json.JSONArray(response.body?.string().orEmpty())
            return (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
        }
    }

    /** Вызывает конкретный инструмент ErekeAI напрямую, без прогона через LLM. */
    fun executeTool(name: String, args: Map<String, String>): ToolExecutionResult {
        val body = JSONObject().apply {
            put("name", name)
            put("args", JSONObject().apply { args.forEach { (k, v) -> put(k, v) } })
        }
        val result = post("/tools/execute", body)
        return ToolExecutionResult(result.optBoolean("success"), result.optString("content"))
    }

    private fun get(path: String): JSONObject {
        val request = buildRequest(path).get().build()
        httpClient.newCall(request).execute().use { response -> return JSONObject(response.body?.string().orEmpty()) }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val request = buildRequest(path).post(body.toString().toRequestBody("application/json".toMediaType())).build()
        httpClient.newCall(request).execute().use { response -> return JSONObject(response.body?.string().orEmpty()) }
    }

    private fun buildRequest(path: String): Request.Builder {
        val builder = Request.Builder().url("${baseUrl.trimEnd('/')}$path")
        authToken?.let { builder.addHeader("X-Ereke-Token", it) }
        return builder
    }
}
