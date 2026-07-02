package com.erekeai.server

import com.erekeai.domain.tool.ToolRegistry
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "MCP Server" — ErekeAI сам выступает MCP-сервером (Model Context Protocol,
 * modelcontextprotocol.io): любой MCP-совместимый клиент (Claude Desktop, другой ваш проект,
 * IDE-плагин) в той же сети может подключиться к http://<телефон>:8766/mcp и увидеть все
 * инструменты ErekeAI (github_action, git_ops, web_search, run_static_checks и т.д.) как
 * СВОИ собственные MCP tools — то есть ErekeAI становится "ядром для других проектов" в
 * терминах самого протокола MCP, а не только через кастомный REST (см. [ErekeApiServer], если
 * добавлен). Реализует минимальный набор JSON-RPC 2.0 методов: initialize, tools/list, tools/call.
 * Аутентификация — необязательный [authToken] (заголовок X-Ereke-Token), т.к. не все MCP-клиенты
 * позволяют легко задавать произвольные заголовки — используйте в доверенной сети.
 */
@Singleton
class ErekeMcpServer @Inject constructor(
    private val toolRegistry: ToolRegistry
) : NanoHTTPD(PORT) {

    var authToken: String? = null

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST || session.uri != "/mcp") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"use POST /mcp"}""")
        }
        authToken?.let { required ->
            if (session.headers["x-ereke-token"] != required) {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", """{"error":"unauthorized"}""")
            }
        }

        val map = HashMap<String, String>()
        session.parseBody(map)
        val body = map["postData"] ?: "{}"

        return try {
            val request = JSONObject(body)
            val method = request.optString("method")
            val id = request.opt("id")
            val params = request.optJSONObject("params") ?: JSONObject()

            val result = when (method) {
                "initialize" -> JSONObject().apply {
                    put("protocolVersion", "2024-11-05")
                    put("serverInfo", JSONObject().apply { put("name", "ErekeAI"); put("version", "0.4.0") })
                    put("capabilities", JSONObject().apply { put("tools", JSONObject()) })
                }
                "tools/list" -> JSONObject().apply {
                    val arr = JSONArray()
                    toolRegistry.all().forEach { tool ->
                        arr.put(JSONObject().apply {
                            put("name", tool.definition.name)
                            put("description", tool.definition.description)
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject().apply {
                                    tool.definition.parameters.forEach { p ->
                                        put(p.name, JSONObject().apply { put("type", "string"); put("description", p.description) })
                                    }
                                })
                            })
                        })
                    }
                    put("tools", arr)
                }
                "tools/call" -> {
                    val toolName = params.optString("name")
                    val argsObj = params.optJSONObject("arguments") ?: JSONObject()
                    val toolArgs = argsObj.keys().asSequence().associateWith { k -> argsObj.optString(k) }
                    val tool = toolRegistry.find(toolName)
                    val toolResult = if (tool == null) {
                        com.erekeai.domain.tool.ToolResult(false, "Инструмент '$toolName' не найден")
                    } else {
                        runBlocking { tool.execute(toolArgs) }
                    }
                    JSONObject().apply {
                        put("content", JSONArray().put(JSONObject().apply { put("type", "text"); put("text", toolResult.content) }))
                        put("isError", !toolResult.success)
                    }
                }
                else -> null
            }

            val response = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", id)
                if (result != null) put("result", result) else put("error", JSONObject().apply { put("code", -32601); put("message", "Method not found: $method") })
            }
            newFixedLengthResponse(Response.Status.OK, "application/json", response.toString())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":${JSONObject.quote(e.message ?: "error")}}""")
        }
    }

    companion object {
        const val PORT = 8766
    }
}
