package com.erekeai.data.multiagent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiAgentEngine @Inject constructor(

    private val selector: AgentSelector,
    private val voting: AgentVoting

) {

    suspend fun execute(
        task: AgentTask
    ): AgentAnswer? {

        val answers = mutableListOf<AgentAnswer>()

        // Здесь позже будут вызовы:
        // GPT
        // Claude
        // Gemini
        // DeepSeek
        // Qwen
        // Llama (offline)

        return voting.vote(answers)
    }

}
