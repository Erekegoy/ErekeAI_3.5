package com.erekeai.domain.workflow

/**
 * ✅ "Workflow Engine" — выполняет цепочки действий, где каждый шаг — вызов инструмента из
 * общего [com.erekeai.domain.tool.ToolRegistry]. Плейсхолдер "{{prev}}" в значении аргумента
 * подставляет текстовый результат предыдущего шага — это то, что делает цепочку осмысленной.
 */
interface WorkflowEngine {
    suspend fun createWorkflow(name: String, steps: List<WorkflowStep>): Long
    suspend fun listWorkflows(): List<Workflow>
    suspend fun getWorkflow(name: String): Workflow?
    suspend fun deleteWorkflow(id: Long)
    suspend fun run(workflow: Workflow): List<WorkflowStepResult>
}
