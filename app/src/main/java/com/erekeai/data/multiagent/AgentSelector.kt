package com.erekeai.data.multiagent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentSelector @Inject constructor() {

    fun select(
        task: AgentTask
    ): List<AgentProvider> {

        return listOf(
            AgentProvider.LOCAL
        )

    }
}
