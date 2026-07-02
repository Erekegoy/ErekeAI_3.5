package com.erekeai.domain.mcp

data class McpServerConfig(val id: String, val name: String, val url: String, val bearerToken: String?)
data class McpToolInfo(val name: String, val description: String, val inputSchemaJson: String)

/** ✅ "MCP Server/Client" — хранилище подключений к ВНЕШНИМ MCP-серверам (роль клиента). */
interface McpServerStore {
    suspend fun getServers(): List<McpServerConfig>
    suspend fun saveServer(config: McpServerConfig)
    suspend fun deleteServer(id: String)
}
