package com.erekeai.domain.tool

/**
 * Контракт инструмента, который может вызвать AI Agent.
 * Реализации живут в data/tools/* (веб-поиск, файлы, калькулятор и т.д.).
 */
interface Tool {
    val definition: ToolDefinition

    /** Выполняет действие инструмента. [args] — пары "имя параметра" → "значение" из ответа модели. */
    suspend fun execute(args: Map<String, String>): ToolResult
}

/**
 * Реестр всех зарегистрированных инструментов. Заполняется через Hilt (см. ToolModule) —
 * это встроенные инструменты. 🟡 Дополнительно поддерживает динамическую регистрацию инструментов
 * из установленных плагинов ([com.erekeai.domain.plugin.PluginRepository]) поверх статического
 * списка — без пересборки приложения.
 */
class ToolRegistry(staticTools: List<Tool>) {
    private val dynamicTools = mutableListOf<Tool>()
    private val staticToolsList = staticTools

    fun all(): List<Tool> = staticToolsList + dynamicTools

    fun find(name: String): Tool? = all().firstOrNull { it.definition.name.equals(name, ignoreCase = true) }

    /** Добавляет инструмент плагина в реестр во время выполнения (см. InstallPluginTool). */
    @Synchronized
    fun registerDynamic(tool: Tool) {
        dynamicTools.removeAll { it.definition.name == tool.definition.name }
        dynamicTools.add(tool)
    }

    @Synchronized
    fun unregisterDynamic(name: String) {
        dynamicTools.removeAll { it.definition.name == name }
    }

    /** Человекочитаемое описание всех инструментов — вставляется в системный промпт агента. */
    fun describeForPrompt(): String = all().joinToString("\n") { tool ->
        val params = tool.definition.parameters.joinToString(", ") {
            "${it.name}${if (!it.required) "?" else ""}: ${it.description}"
        }
        "- ${tool.definition.name}(${params}): ${tool.definition.description}"
    }
}
