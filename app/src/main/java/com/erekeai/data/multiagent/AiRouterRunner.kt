package com.erekeai.data.multiagent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRouterRunner @Inject constructor(
) : AgentRunner {

    override suspend fun run(
        task: AgentTask
    ): AgentAnswer {

        return AgentAnswer(
            provider = AgentProvider.LOCAL,
            answer = "",
            success = false,
            score = 0
        )

    }
}
