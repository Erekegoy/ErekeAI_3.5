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

        var retry = 0

        while (retry < task.maxRetries) {

            workflow.forEachIndexed { index, step ->

                state = state.copy(
                    currentStep = index + 1,
                    mode = when (step) {
                        DeveloperWorkflow.ANALYZE_PROJECT -> DeveloperMode.ANALYZE
                        DeveloperWorkflow.CREATE_PLAN -> DeveloperMode.PLAN
                        DeveloperWorkflow.BUILD -> DeveloperMode.BUILD
                        DeveloperWorkflow.TEST -> DeveloperMode.TEST
                        DeveloperWorkflow.FIX_ERRORS -> DeveloperMode.FIX
                        DeveloperWorkflow.COMMIT -> DeveloperMode.COMMIT
                        DeveloperWorkflow.PUSH -> DeveloperMode.PUSH
                        DeveloperWorkflow.FINISHED -> DeveloperMode.DONE
                        else -> DeveloperMode.EDIT
                    }
                )

                // Пока инструменты не вызываем.
                // Их будет запускать DeveloperWorkflowTool,
                // чтобы не создавать циклическую зависимость Hilt.
            }

            retry++

            // Пока один проход.
            // Следующим этапом заменим на анализ результата Build.
            break
        }

        return state.copy(
            mode = DeveloperMode.DONE,
            success = true
        )
    }

    override suspend fun next(
        result: WorkflowResult
    ): Boolean {

        if (result.success) {
            return false
        }

        if (result.retry) {
            return true
        }

        return false
    }
}
