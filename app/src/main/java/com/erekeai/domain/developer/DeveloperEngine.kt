package com.erekeai.domain.developer

import com.erekeai.data.developer.DeveloperState
import com.erekeai.data.developer.DeveloperTask
import com.erekeai.data.developer.WorkflowResult

interface DeveloperEngine {

    suspend fun run(
        task: DeveloperTask
    ): DeveloperState

    suspend fun next(
        result: WorkflowResult
    ): Boolean
}
