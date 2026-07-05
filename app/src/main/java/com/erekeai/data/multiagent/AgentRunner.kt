package com.erekeai.data.multiagent

interface AgentRunner {

    suspend fun run(
        task: AgentTask
    ): AgentAnswer

}
