package com.erekeai.data.tools.dev

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import com.erekeai.executor.RetryingFixExecutor
import com.erekeai.executor.FixOutcome

import com.erekeai.domain.developer.DeveloperEngine
import com.erekeai.data.developer.DeveloperTask

@Singleton
class DeveloperWorkflowTool @Inject constructor(
    private val analyzeProjectTool: AnalyzeProjectTool,
    private val codeSearchTool: CodeSearchTool,
    private val writeFileTool: WriteFileTool,
    private val staticCheckTool: StaticCheckTool,
    private val analyzeLogTool: AnalyzeLogTool,
    private val buildApkAgentTool: BuildApkAgentTool,
    private val gitOpsTool: GitOpsTool,
    private val retryingFixExecutor: RetryingFixExecutor
    private val developerEngine: DeveloperEngine
) : Tool {

    override val definition = ToolDefinition(
        name = "developer_workflow",
        description = "Автоматический цикл разработки проекта",
        parameters = listOf(
            ToolParameter("task", "Описание задачи"),
            ToolParameter("project", "Имя проекта", false),
            ToolParameter("commit", "true/false", false),
            ToolParameter("push", "true/false", false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {

    val task = args["task"] ?: return ToolResult(
        false,
        "Не указана задача"
    )

        when (args["action"]) {
        
        "developer_mode" -> {

    val state =
        developerEngine.run(

            DeveloperTask(

                title = task,

                description = task,

                autoBuild = true,

                autoFix = true,

                autoCommit =
                    args["commit"] == "true",

                autoPush =
                    args["push"] == "true"

            )

        )

    return ToolResult(

        state.success,

        "Developer Mode завершён.\n" +
        "Mode=${state.mode}\n" +
        "Step=${state.currentStep}"

    )

}

    "fix_build" -> {
        val filePath = args["file"] ?: return ToolResult(false, "Не указан file")
        val log = args["log"] ?: ""

        return when (
            val result = retryingFixExecutor.run(filePath, log)
        ) {
            is FixOutcome.Success ->
                ToolResult(true, "Исправлено за ${result.attempts} попыток")

            is FixOutcome.GaveUp ->
                ToolResult(false, result.lastBuildLog)

            is FixOutcome.CancelledByUser ->
                ToolResult(false, "Отменено пользователем")

            is FixOutcome.Failed ->
                ToolResult(false, result.reason)
        }
    }
}

    val log = StringBuilder()

    log.appendLine("🚀 Developer Workflow")
    log.appendLine()
    log.appendLine("Task:")
    log.appendLine(task)
    log.appendLine()

    log.appendLine("1. Анализ проекта...")
    analyzeProjectTool.execute(emptyMap())

    log.appendLine("2. Поиск нужного кода...")
    codeSearchTool.execute(
        mapOf(
            "query" to task
        )
    )

    log.appendLine("3. Подготовка изменений...")

    if (args["commit"] == "true") {

        log.appendLine("4. Git Commit")

        gitOpsTool.execute(
            mapOf(
                "action" to "commit",
                "dir_name" to "current",
                "message" to task
            )
        )
    }

    if (args["push"] == "true") {

        log.appendLine("5. Git Push")

        gitOpsTool.execute(
            mapOf(
                "action" to "push",
                "dir_name" to "current"
            )
        )
    }

    return ToolResult(
        true,
        log.toString()
    )
}

}       
