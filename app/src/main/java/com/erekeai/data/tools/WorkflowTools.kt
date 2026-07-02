package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import com.erekeai.domain.workflow.WorkflowEngine
import com.erekeai.domain.workflow.WorkflowStep
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateWorkflowTool @Inject constructor(private val engine: WorkflowEngine) : Tool {
    override val definition = ToolDefinition(
        name = "create_workflow",
        description = "Сохраняет многошаговый сценарий (цепочку вызовов инструментов) под именем для запуска через run_workflow",
        parameters = listOf(
            ToolParameter("name", "уникальное имя сценария"),
            ToolParameter("steps_json", "JSON-массив [{\"tool\":\"web_search\",\"args\":{\"query\":\"...\"}}]. {{prev}} подставляет результат предыдущего шага.")
        )
    )
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val name = args["name"]?.trim() ?: return ToolResult(false, "Не указан 'name'")
        val stepsJson = args["steps_json"] ?: return ToolResult(false, "Не указан 'steps_json'")
        return try {
            val arr = JSONArray(stepsJson)
            val steps = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val argsObj = obj.optJSONObject("args")
                val stepArgs = argsObj?.keys()?.asSequence()?.associateWith { k -> argsObj.optString(k) } ?: emptyMap()
                WorkflowStep(obj.getString("tool"), stepArgs)
            }
            engine.createWorkflow(name, steps)
            ToolResult(true, "Сценарий '$name' сохранён (${steps.size} шаг(ов))")
        } catch (e: Exception) {
            ToolResult(false, "Ошибка разбора steps_json: ${e.message}")
        }
    }
}

@Singleton
class RunWorkflowTool @Inject constructor(private val engine: WorkflowEngine) : Tool {
    override val definition = ToolDefinition(
        name = "run_workflow",
        description = "Запускает сохранённый сценарий по имени",
        parameters = listOf(ToolParameter("name", "имя сценария"))
    )
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val name = args["name"]?.trim() ?: return ToolResult(false, "Не указан 'name'")
        val workflow = engine.getWorkflow(name) ?: return ToolResult(false, "Сценарий '$name' не найден")
        val results = engine.run(workflow)
        return ToolResult(results.all { it.success }, results.mapIndexed { i, r ->
            "${if (r.success) "✅" else "⚠️"} Шаг ${i + 1} (${r.toolName}): ${r.content.take(300)}"
        }.joinToString("\n"))
    }
}

@Singleton
class ListWorkflowsTool @Inject constructor(private val engine: WorkflowEngine) : Tool {
    override val definition = ToolDefinition(name = "list_workflows", description = "Показывает список сохранённых сценариев", parameters = emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val workflows = engine.listWorkflows()
        return if (workflows.isEmpty()) ToolResult(true, "Сценариев пока нет.")
        else ToolResult(true, workflows.joinToString("\n") { "- ${it.name} (${it.steps.size} шаг(ов))" })
    }
}
