package com.erekeai.data.developer

import com.erekeai.domain.developer.DeveloperEngine as IDeveloperEngine
import com.erekeai.domain.project.ProjectManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperEngine @Inject constructor(

    private val projectManager: ProjectManager

) : IDeveloperEngine {

    override suspend fun run(
        task: DeveloperTask
    ): DeveloperState {

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
            DeveloperWorkflow.TEST,
            DeveloperWorkflow.COMMIT,
            DeveloperWorkflow.PUSH,
            DeveloperWorkflow.FINISHED
        )

        workflow.forEachIndexed { index, step ->

            state = state.copy(
                currentStep = index + 1,
                mode = when (step) {
                    DeveloperWorkflow.ANALYZE_PROJECT -> DeveloperMode.ANALYZE
                    DeveloperWorkflow.CREATE_PLAN -> DeveloperMode.PLAN
                    else -> DeveloperMode.EDIT
                }
            )

            // Пока только фиксируем прохождение этапов.
            // Реальные вызовы инструментов подключим позже
            // через DeveloperWorkflowTool, чтобы не создавать
            // циклическую зависимость Hilt.
        }

        return state.copy(
            mode = DeveloperMode.DONE,
            success = true
        )
    }
}
