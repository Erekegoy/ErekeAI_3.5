package com.erekeai.domain.usecase

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.Role
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.repository.ChatRepository
import com.erekeai.domain.router.AiRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Запускает задачу пользователя в режиме AI Agent: сохраняет сообщение пользователя,
 * прогоняет ReAct-цикл через [AgentOrchestrator], сохраняя каждый шаг (вызов инструмента,
 * его результат, финальный ответ) в историю диалога, чтобы UI могло показать весь ход работы агента.
 * Если включён 🟡 AI Router (Настройки), провайдер выбирается автоматически по содержанию задачи.
 */
class RunAgentTaskUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val aiRouter: AiRouter,
    private val providerRegistry: AiProviderRegistry,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(
        conversationId: Long,
        text: String,
        provider: AiProviderType,
        /** Персона агента — GENERAL для обычного чата, DEVELOPER для 🤖 AI Developer Agent. */
        persona: AgentPersona = AgentPersona.GENERAL
    ): Flow<AgentEvent> = flow {
        require(text.isNotBlank()) { "Задача не может быть пустой" }

        val resolvedProvider = resolveProvider(text, provider)
        chatRepository.appendMessage(conversationId, Role.USER, text, resolvedProvider.id)
        val history = chatRepository.getHistorySnapshot(conversationId)

        agentOrchestrator.run(history, resolvedProvider, persona = persona).collect { event ->
            when (event) {
                is AgentEvent.ToolCall -> {
                    val label = "🔧 ${event.toolName}(${event.args.entries.joinToString { "${it.key}=${it.value}" }})"
                    chatRepository.appendMessage(conversationId, Role.TOOL, label, resolvedProvider.id)
                }
                is AgentEvent.ToolResult -> {
                    val prefix = if (event.success) "✅" else "⚠️"
                    chatRepository.appendMessage(conversationId, Role.TOOL, "$prefix ${event.content}", resolvedProvider.id)
                }
                is AgentEvent.FinalAnswer -> {
                    chatRepository.appendMessage(conversationId, Role.ASSISTANT, event.text, resolvedProvider.id)
                }
                is AgentEvent.Error -> {
                    chatRepository.appendMessage(conversationId, Role.SYSTEM, event.message, resolvedProvider.id)
                }
                is AgentEvent.Thinking -> { /* не сохраняем в БД, только для наблюдения в UI-потоке */ }
            }
            emit(event)
        }
    }

    private suspend fun resolveProvider(text: String, manualProvider: AiProviderType): AiProviderType {
        if (!settingsDataStore.isAiRouterEnabled.first()) return manualProvider
        val available = providerRegistry.availableProviders().filter { it.isConfigured() }.map { it.type }
        if (available.isEmpty()) return manualProvider
        return aiRouter.route(text, hasImage = false, availableProviders = available).provider
    }
}
