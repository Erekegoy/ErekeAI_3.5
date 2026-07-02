package com.erekeai.domain.agent

/**
 * Шаг работы AI Agent, эмитится потоком по мере выполнения ReAct-цикла
 * (Thought → Action → Observation → ... → Final Answer), чтобы UI могло
 * показать пользователю "ход мыслей" и вызовы инструментов, а не просто ждать финальный ответ.
 */
sealed class AgentEvent {
    /** Промежуточное рассуждение модели перед выбором действия. */
    data class Thinking(val text: String) : AgentEvent()

    /** Модель решила вызвать инструмент. */
    data class ToolCall(val toolName: String, val args: Map<String, String>) : AgentEvent()

    /** Результат выполнения инструмента. */
    data class ToolResult(val toolName: String, val success: Boolean, val content: String) : AgentEvent()

    /** Финальный ответ агента пользователю — завершает цикл. */
    data class FinalAnswer(val text: String) : AgentEvent()

    /** Ошибка на любом из шагов (например, превышен лимит итераций). */
    data class Error(val message: String) : AgentEvent()
}
