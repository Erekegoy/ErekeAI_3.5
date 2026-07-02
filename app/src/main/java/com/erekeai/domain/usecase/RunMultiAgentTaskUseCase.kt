package com.erekeai.domain.usecase

import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.Role
import com.erekeai.domain.planner.MultiAgentPlanner
import com.erekeai.domain.planner.PlannerEvent
import com.erekeai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Запускает сложную задачу через [MultiAgentPlanner]: план → подзадачи (суб-агенты) → сводный
 * отчёт, сохраняя каждый шаг в историю диалога (аналогично [RunAgentTaskUseCase], но с
 * промежуточными "🗂 План" и "🧩 Подзадача" сообщениями).
 */
class RunMultiAgentTaskUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val multiAgentPlanner: MultiAgentPlanner
) {
    suspend operator fun invoke(
        conversationId: Long,
        goal: String,
        provider: AiProviderType,
        persona: AgentPersona = AgentPersona.GENERAL
    ): Flow<PlannerEvent> = flow {
        require(goal.isNotBlank()) { "Цель не может быть пустой" }
        chatRepository.appendMessage(conversationId, Role.USER, goal, provider.id)

        multiAgentPlanner.run(goal, provider, persona).collect { event ->
            when (event) {
                is PlannerEvent.PlanCreated -> {
                    val text = "🗂 План (${event.subTasks.size} подзадач):\n" +
                        event.subTasks.joinToString("\n") { "${it.id}. ${it.title}" }
                    chatRepository.appendMessage(conversationId, Role.TOOL, text, provider.id)
                }
                is PlannerEvent.SubTaskStarted -> {
                    chatRepository.appendMessage(conversationId, Role.TOOL, "🧩 Выполняю: ${event.subTask.title}", provider.id)
                }
                is PlannerEvent.SubTaskFinished -> {
                    val icon = if (event.success) "✅" else "⚠️"
                    chatRepository.appendMessage(conversationId, Role.TOOL, "$icon ${event.subTask.title}: ${event.result.take(300)}", provider.id)
                }
                is PlannerEvent.FinalReport -> {
                    chatRepository.appendMessage(conversationId, Role.ASSISTANT, event.text, provider.id)
                }
                is PlannerEvent.Error -> {
                    chatRepository.appendMessage(conversationId, Role.SYSTEM, event.message, provider.id)
                }
            }
            emit(event)
        }
    }
}
