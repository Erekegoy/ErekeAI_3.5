package com.erekeai.data.developer

import com.erekeai.domain.tool.ToolRegistry
import com.erekeai.domain.developer.DeveloperEngine as IDeveloperEngine
import com.erekeai.domain.project.ProjectManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperEngine @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val projectManager: ProjectManager
) : IDeveloperEngine {

    override suspend fun run(task: DeveloperTask): DeveloperState {

        var state = DeveloperState(
            mode = DeveloperMode.ANALYZE,
            currentTask = task.title
        )

        val workflow = listOf(
            DeveloperWorkflow.ANALYZE_PROJECT,
            DeveloperWorkflow.CREATE_PLAN,
            DeveloperWorkflow.SEARCH_CODE,
            DeveloperWorkflow.READ_FILE,
            DeveloperWorkflow.MODIFY_FILE,
            DeveloperWorkflow.BUILD,
            DeveloperWorkflow.ANALYZE_LOG,
            DeveloperWorkflow.FIX_ERRORS,
            DeveloperWorkflow.TEST
        )

        var retry = 0

        while (retry < task.maxRetries) {

            workflow.forEachIndexed { index, step ->

                state = state.copy(
                    currentStep = index + 1,
                    mode = DeveloperMode.EDIT
                )

                when (step) {

    DeveloperWorkflow.ANALYZE_PROJECT -> {
        toolRegistry.find("analyze_project")
            ?.execute(emptyMap())
    }

    DeveloperWorkflow.CREATE_PLAN -> {
        toolRegistry.find("create_dev_plan")
            ?.execute(
                mapOf(
                    "task" to task.description
                )
            )
    }

    DeveloperWorkflow.SEARCH_CODE -> {
        toolRegistry.find("code_search")
            ?.execute(
                mapOf(
                    "query" to task.description
                )
            )
    }

    DeveloperWorkflow.READ_FILE -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.MODIFY_FILE -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.BUILD -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.ANALYZE_LOG -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.FIX_ERRORS -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.TEST -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.COMMIT -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.PUSH -> {
        // будет реализовано позже
    }

    DeveloperWorkflow.FINISHED -> {
        // завершение workflow
    }
}
            }

            retry++

            break
        }

        return state.copy(
            mode = DeveloperMode.DONE,
            success = true
        )
    }
}
