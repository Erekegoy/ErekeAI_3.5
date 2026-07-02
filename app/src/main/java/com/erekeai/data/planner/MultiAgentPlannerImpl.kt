package com.erekeai.data.planner

import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Role
import com.erekeai.domain.planner.MultiAgentPlanner
import com.erekeai.domain.planner.PlannerEvent
import com.erekeai.domain.planner.SubTask
import com.erekeai.domain.repository.AiProviderRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiAgentPlannerImpl @Inject constructor(
    private val providerRegistry: AiProviderRegistry,
    private val agentOrchestrator: AgentOrchestrator
) : MultiAgentPlanner {

    override fun run(
        goal: String,
        provider: AiProviderType,
        persona: AgentPersona,
        maxSubTasks: Int
    ): Flow<PlannerEvent> = flow {
        val aiProvider = providerRegistry.getProvider(provider)

        // Шаг 1: планировщик просит модель разбить цель на подзадачи в строгом JSON-формате.
        val planningPrompt = """
            Разбей следующую цель пользователя на $maxSubTasks или меньше конкретных, независимо
            выполнимых подзадач. Ответь СТРОГО JSON-массивом без какого-либо другого текста, формат:
            [{"title": "короткое название", "instructions": "подробная инструкция для исполнителя"}]

            Цель: $goal
        """.trimIndent()

        val planResponse = StringBuilder()
        try {
            aiProvider.streamReply(listOf(ChatMessage(conversationId = 0L, role = Role.SYSTEM, text = planningPrompt)))
                .collect { chunk -> planResponse.append(chunk) }
        } catch (e: Exception) {
            emit(PlannerEvent.Error("Ошибка построения плана: ${e.message}"))
            return@flow
        }

        val subTasks = parseSubTasks(planResponse.toString())
        if (subTasks.isEmpty()) {
            emit(PlannerEvent.Error("Не удалось разобрать план подзадач из ответа модели"))
            return@flow
        }
        emit(PlannerEvent.PlanCreated(subTasks))

        // Шаг 2: выполняем каждую подзадачу отдельным запуском AgentOrchestrator (суб-агент).
        val subResults = mutableListOf<String>()
        for (subTask in subTasks) {
            emit(PlannerEvent.SubTaskStarted(subTask))
            val subHistory = listOf(ChatMessage(conversationId = 0L, role = Role.USER, text = subTask.instructions))
            var subFinal = ""
            var subSuccess = true
            try {
                agentOrchestrator.run(subHistory, provider, maxSteps = 5, persona = persona).collect { event ->
                    when (event) {
                        is AgentEvent.FinalAnswer -> subFinal = event.text
                        is AgentEvent.Error -> { subFinal = event.message; subSuccess = false }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                subFinal = "Ошибка суб-агента: ${e.message}"
                subSuccess = false
            }
            subResults.add("## ${subTask.title}\n$subFinal")
            emit(PlannerEvent.SubTaskFinished(subTask, subFinal, subSuccess))
        }

        // Шаг 3: сводим результаты всех подзадач в единый ответ пользователю.
        val aggregationPrompt = """
            Ты свёл результаты работы нескольких суб-агентов по общей цели пользователя.
            Цель: $goal

            Результаты подзадач:
            ${subResults.joinToString("\n\n")}

            Составь единый связный итоговый ответ пользователю на русском языке, без служебных
            пометок вида "Thought"/"Action" — просто финальный текст.
        """.trimIndent()

        val finalReport = StringBuilder()
        try {
            aiProvider.streamReply(listOf(ChatMessage(conversationId = 0L, role = Role.SYSTEM, text = aggregationPrompt)))
                .collect { chunk -> finalReport.append(chunk) }
        } catch (e: Exception) {
            emit(PlannerEvent.FinalReport(subResults.joinToString("\n\n"))) // fallback — хотя бы сырые результаты
            return@flow
        }
        emit(PlannerEvent.FinalReport(finalReport.toString()))
    }

    private fun parseSubTasks(raw: String): List<SubTask> {
        val jsonStart = raw.indexOf('[')
        val jsonEnd = raw.lastIndexOf(']')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd < jsonStart) return emptyList()
        return try {
            val arr = JSONArray(raw.substring(jsonStart, jsonEnd + 1))
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SubTask(id = i + 1, title = obj.optString("title", "Подзадача ${i + 1}"), instructions = obj.optString("instructions"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
