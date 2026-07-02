package com.erekeai.data.tools.mcp

import com.erekeai.data.mcp.McpClient
import com.erekeai.domain.mcp.McpServerStore
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpListServersTool @Inject constructor(private val store: McpServerStore) : Tool {
    override val definition = ToolDefinition("mcp_list_servers", "Показывает список подключённых MCP-серверов", emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val servers = store.getServers()
        return if (servers.isEmpty()) ToolResult(true, "MCP-серверы не настроены (Настройки → MCP).")
        else ToolResult(true, servers.joinToString("\n") { "- ${it.name} (id=${it.id}, ${it.url})" })
    }
}

@Singleton
class McpListToolsTool @Inject constructor(
    private val store: McpServerStore,
    private val httpClient: OkHttpClient
) : Tool {
    override val definition = ToolDefinition(
        "mcp_list_tools", "Показывает инструменты конкретного MCP-сервера",
        listOf(ToolParameter("server_id", "id сервера из mcp_list_servers"))
    )
    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val serverId = args["server_id"] ?: return@withContext ToolResult(false, "Не указан 'server_id'")
        val config = store.getServers().firstOrNull { it.id == serverId } ?: return@withContext ToolResult(false, "Сервер не найден")
        val client = McpClient(config.url, config.bearerToken, httpClient)
        if (!client.initialize()) return@withContext ToolResult(false, "Не удалось подключиться к '${config.name}'")
        val tools = client.listTools()
        if (tools.isEmpty()) ToolResult(true, "Инструментов нет") else ToolResult(true, tools.joinToString("\n") { "- ${it.name}: ${it.description}" })
    }
}

@Singleton
class McpCallToolTool @Inject constructor(
    private val store: McpServerStore,
    private val httpClient: OkHttpClient
) : Tool {
    override val definition = ToolDefinition(
        "mcp_call_tool", "Вызывает инструмент на MCP-сервере",
        listOf(
            ToolParameter("server_id", "id сервера"),
            ToolParameter("tool_name", "имя инструмента на сервере"),
            ToolParameter("arguments_json", "JSON-объект аргументов")
        )
    )
    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val serverId = args["server_id"] ?: return@withContext ToolResult(false, "Не указан 'server_id'")
        val toolName = args["tool_name"] ?: return@withContext ToolResult(false, "Не указан 'tool_name'")
        val argumentsJson = args["arguments_json"]?.ifBlank { "{}" } ?: "{}"
        val config = store.getServers().firstOrNull { it.id == serverId } ?: return@withContext ToolResult(false, "Сервер не найден")
        val client = McpClient(config.url, config.bearerToken, httpClient)
        try { ToolResult(true, client.callTool(toolName, JSONObject(argumentsJson))) }
        catch (e: Exception) { ToolResult(false, "Ошибка: ${e.message}") }
    }
}
