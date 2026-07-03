package com.erekeai.domain.tool

/**
 * Контракт инструмента, который может вызвать AI Agent.
 * Реализации находятся в пакете data.tools.
 */
interface Tool {
    val definition: ToolDefinition

    /**
     * Выполняет действие инструмента.
     * args — пары "имя параметра" → "значение", полученные от модели.
     */
    suspend fun execute(args: Map<String, String>): ToolResult
}

/**
 * Реестр всех зарегистрированных инструментов.
 * Заполняется через Hilt (см. ToolModule).
 *
 * Помимо встроенных инструментов поддерживает
 * динамическую регистрацию инструментов плагинов.
 */
class ToolRegistry(
    staticTools: List<Tool>
) {

    private val dynamicTools = mutableListOf<Tool>()
    private val staticToolsList = staticTools

    fun all(): List<Tool> = staticToolsList + dynamicTools

    fun find(name: String): Tool? =
        all().firstOrNull {
            it.definition.name.equals(name, ignoreCase = true)
        }

    @Synchronized
    fun registerDynamic(tool: Tool) {
        dynamicTools.removeAll {
            it.definition.name == tool.definition.name
        }
        dynamicTools.add(tool)
    }

    @Synchronized
    fun unregisterDynamic(name: String) {
        dynamicTools.removeAll {
            it.definition.name == name
        }
    }

    fun describeForPrompt(): String =
        all().joinToString("\n") { tool ->
            val params = tool.definition.parameters.joinToString(", ") {
                "${it.name}${if (it.required) "" else "?"}: ${it.description}"
            }

            "- ${tool.definition.name}($params): ${tool.definition.description}"
        }
}
