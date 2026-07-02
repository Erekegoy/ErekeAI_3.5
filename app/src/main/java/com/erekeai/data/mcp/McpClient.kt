package com.erekeai.data.mcp

import com.erekeai.domain.mcp.McpToolInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Клиент JSON-RPC 2.0 поверх MCP "Streamable HTTP" транспорта: initialize, tools/list, tools/call.
 * Подключается к УЖЕ ЗАПУЩЕННОМУ где-то MCP-серверу (ПК/VPS/облачный эндпоинт) — на самом
 * телефоне поднять stdio MCP-сервер (`npx ...`) нельзя (нет Node.js рантайма).
 */
class McpClient(
    private val serverUrl: String,
    private val bearerToken: String?,
    private val httpClient: OkHttpClient
) {
    private val nextId = AtomicInteger(1)

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        val params = JSONObject().apply {
            put("protocolVersion", "2024-11-05")
            put("capabilities", JSONObject())
            put("clientInfo", JSONObject().apply { put("name", "ErekeAI"); put("version", "0.4.0") })
        }
        postRpc("initialize", params) != null
    }

    suspend fun listTools(): List<McpToolInfo> = withContext(Dispatchers.IO) {
        val result = postRpc("tools/list", JSONObject()) ?: return@withContext emptyList()
        val tools = result.optJSONArray("tools") ?: return@withContext emptyList()
        (0 until tools.length()).map { i ->
            val obj = tools.getJSONObject(i)
            McpToolInfo(obj.optString("name"), obj.optString("description"), obj.optJSONObject("inputSchema")?.toString() ?: "{}")
        }
    }

    suspend fun callTool(name: String, argumentsJson: JSONObject): String = withContext(Dispatchers.IO) {
        val params = JSONObject().apply { put("name", name); put("arguments", argumentsJson) }
        val result = postRpc("tools/call", params) ?: return@withContext "MCP-сервер не ответил"
        val content = result.optJSONArray("content") ?: return@withContext result.toString()
        (0 until content.length()).joinToString("\n") { i -> content.getJSONObject(i).optString("text", content.getJSONObject(i).toString()) }
    }

    private fun postRpc(method: String, params: JSONObject): JSONObject? {
        val id = nextId.getAndIncrement()
        val body = JSONObject().apply { put("jsonrpc", "2.0"); put("id", id); put("method", method); put("params", params) }.toString()
        val builder = Request.Builder().url(serverUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json, text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
        bearerToken?.takeIf { it.isNotBlank() }?.let { builder.addHeader("Authorization", "Bearer $it") }
        return try {
            httpClient.newCall(builder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful || raw.isBlank()) return null
                val jsonText = if (raw.trimStart().startsWith("data:")) raw.substringAfter("data:").trim() else raw
                JSONObject(jsonText).optJSONObject("result")
            }
        } catch (e: Exception) { null }
    }
}
