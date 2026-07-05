package com.erekeai.data.multiagent

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.router.AiRouter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRouterRunner @Inject constructor(

    private val aiRouter: AiRouter

) : AgentRunner {

    override suspend fun run(
        task: AgentTask
    ): AgentAnswer {

        val decision = aiRouter.route(
            taskText = task.prompt,
            hasImage = false,
            availableProviders = listOf(
                AiProviderType.GEMINI,
                AiProviderType.OPENAI,
                AiProviderType.GROQ,
                AiProviderType.OLLAMA
            )
        )

        return AgentAnswer(
            provider = decision.provider.toAgentProvider(),
            model = decision.provider.name,
            answer = "",
            score = 100,
            success = true
        )
    }

    private fun AiProviderType.toAgentProvider(): AgentProvider =
        when (this) {
            AiProviderType.OPENAI -> AgentProvider.GPT
            AiProviderType.GEMINI -> AgentProvider.GEMINI
            AiProviderType.GROQ -> AgentProvider.GROK
            AiProviderType.OLLAMA -> AgentProvider.LOCAL
        }
}
