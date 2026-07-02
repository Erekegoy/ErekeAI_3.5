package com.erekeai.domain.agent

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Оркестрирует выполнение задачи пользователя в режиме агента: модель может
 * рассуждать, вызывать инструменты (Tool Calling) и повторять цикл, пока не даст
 * финальный ответ. Не зависит от конкретного AI-провайдера — работает поверх
 * общего интерфейса AiProvider (см. domain.repository.AiProvider).
 */
interface AgentOrchestrator {
    fun run(
        history: List<ChatMessage>,
        provider: AiProviderType,
        maxSteps: Int = 5,
        /** Персона агента — влияет на системный промпт (см. [AgentPersona]). */
        persona: AgentPersona = AgentPersona.GENERAL
    ): Flow<AgentEvent>
}
