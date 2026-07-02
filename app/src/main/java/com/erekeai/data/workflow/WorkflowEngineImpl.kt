package com.erekeai.data.workflow

import com.erekeai.data.local.db.WorkflowDao
import com.erekeai.data.local.db.WorkflowEntity
import com.erekeai.domain.tool.ToolRegistry
import com.erekeai.domain.tool.ToolResult
import com.erekeai.domain.workflow.Workflow
import com.erekeai.domain.workflow.WorkflowEngine
import com.erekeai.domain.workflow.WorkflowStep
import com.erekeai.domain.workflow.WorkflowStepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WorkflowEngineImpl @Inject constructor(
    private val dao: WorkflowDao,
    private val toolRegistry: Provider<ToolRegistry>
) : WorkflowEngine {

    override suspend fun createWorkflow(name: String, steps: List<WorkflowStep>): Long = withContext(Dispatchers.IO) {
        dao.insert(WorkflowEntity(name = name, stepsJson = encode(steps), createdAt = System.currentTimeMillis()))
    }

    override suspend fun listWorkflows(): List<Workflow> = withContext(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }

    override suspend fun getWorkflow(name: String): Workflow? = withContext(Dispatchers.IO) { dao.getByName(name)?.toDomain() }

    override suspend fun deleteWorkflow(id: Long) = withContext(Dispatchers.IO) { dao.delete(id) }

    override suspend fun run(workflow: Workflow): List<WorkflowStepResult> = withContext(Dispatchers.Default) {
        var previousOutput = ""
        workflow.steps.map { step ->
            val tool = toolRegistry.get().find(step.toolName)
            if (tool == null) {
                WorkflowStepResult(step.toolName, false, "Инструмент '${step.toolName}' не найден")
            } else {
                val resolvedArgs = step.args.mapValues { (_, v) -> v.replace("{{prev}}", previousOutput) }
                val result: ToolResult = try { tool.execute(resolvedArgs) } catch (e: Exception) {
                    ToolResult(false, "Ошибка шага '${step.toolName}': ${e.message}")
                }
                previousOutput = result.content
                WorkflowStepResult(step.toolName, result.success, result.content)
            }
        }
    }

    private fun encode(steps: List<WorkflowStep>): String {
        val arr = JSONArray()
        steps.forEach { step ->
            val argsObj = JSONObject(); step.args.forEach { (k, v) -> argsObj.put(k, v) }
            arr.put(JSONObject().apply { put("tool", step.toolName); put("args", argsObj) })
        }
        return arr.toString()
    }

    private fun decode(json: String): List<WorkflowStep> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val argsObj = obj.optJSONObject("args") ?: JSONObject()
            val args = argsObj.keys().asSequence().associateWith { key -> argsObj.optString(key) }
            WorkflowStep(obj.getString("tool"), args)
        }
    }

    private fun WorkflowEntity.toDomain() = Workflow(id, name, decode(stepsJson), createdAt)
}
