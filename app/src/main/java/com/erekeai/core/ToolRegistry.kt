package com.erekeai.core

/**
 * Реестр всех доступных агенту инструментов.
 *
 * Agent/Executor не создают Tool напрямую и не знают о конкретных реализациях
 * (JGit, файловая система и т.д.) — они получают Tool только через registry.get(id).
 *
 * Регистрация всех инструментов происходит один раз при старте приложения (см. DI-модуль).
 */
class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        require(!tools.containsKey(tool.id)) { "Tool with id '${tool.id}' is already registered" }
        tools[tool.id] = tool
    }

    fun get(id: String): Tool =
        tools[id] ?: error("Tool '$id' is not registered")

    fun getOrNull(id: String): Tool? = tools[id]

    fun all(): List<Tool> = tools.values.toList()
}
