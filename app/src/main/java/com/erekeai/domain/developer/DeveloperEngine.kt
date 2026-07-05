package com.erekeai.domain.developer

import com.erekeai.data.developer.DeveloperState
import com.erekeai.data.developer.DeveloperTask

interface DeveloperEngine {

    suspend fun run(
        task: DeveloperTask
    ): DeveloperState

}
