package com.erekeai.data.tools.dev

import com.erekeai.data.developer.DeveloperTask
import com.erekeai.domain.developer.DeveloperEngine
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperWorkflowTool @Inject constructor(
    private val developerEngine: DeveloperEngine
) : Tool {

    override val definition = ToolDefinition(
        name = "developer_workflow",
        description = "Полностью автоматический цикл разработки проекта.",
        parameters = listOf(
            ToolParameter("task", "Описание задачи"),
            ToolParameter("commit", "true/false", false),
            ToolParameter("push", "true/false", false)
        )
    )

    override suspend fun execute(
        args: Map<String, String>
    ): ToolResult {

        val task = args["task"]
            ?: return ToolResult(
                false,
                "Не указана задача."
            )

        val state = developerEngine.run(
            DeveloperTask(
                title = task,
                description = task,
                autoBuild = true,
                autoFix = true,
                autoCommit = args["commit"] == "true",
                autoPush = args["push"] == "true"
            )
        )

        return ToolResult(
            state.success,
            buildString {
                appendLine("🤖 Developer Mode")
                appendLine("Mode: ${state.mode}")
                appendLine("Step: ${state.currentStep}")
                appendLine("Success: ${state.success}")
            }
        )
    }
}

