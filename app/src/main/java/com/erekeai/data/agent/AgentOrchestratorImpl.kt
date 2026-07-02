package com.erekeai.data.agent

import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Role
import com.erekeai.domain.project.ProjectManager
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.tool.ToolRegistry
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentOrchestratorImpl @Inject constructor(
    private val providerRegistry: AiProviderRegistry,
    private val toolRegistry: ToolRegistry,
    private val projectManager: ProjectManager
) : AgentOrchestrator {

    override fun run(
        history: List<ChatMessage>,
        provider: AiProviderType,
        maxSteps: Int,
        persona: AgentPersona
    ): Flow<AgentEvent> = flow {
        val aiProvider = providerRegistry.getProvider(provider)
        val userTask = history.lastOrNull { it.role == Role.USER }?.text.orEmpty()

        // 🟡 "Project Manager": контекст активного проекта (в т.ч. репозиторий по умолчанию для
        // github_action/gitlab_action/build_apk_agent) — best effort, ошибка не должна ронять агента.
        val activeProject = try { projectManager.getActiveProject() } catch (e: Exception) { null }

        // "Блокнот" агента — накапливает Thought/Action/Observation для каждой итерации,
        // чтобы модель видела полный ход рассуждений на следующем шаге.
        val scratchpad = StringBuilder()

        repeat(maxSteps) { step ->
            val prompt = buildPrompt(userTask, scratchpad.toString(), persona, activeProject)
            val syntheticHistory = listOf(
                ChatMessage(conversationId = 0L, role = Role.SYSTEM, text = prompt)
            )

            val rawResponse = StringBuilder()
            aiProvider.streamReply(syntheticHistory).collect { chunk -> rawResponse.append(chunk) }

            val parsed = ReActParser.parse(rawResponse.toString())

            parsed.thought?.takeIf { it.isNotBlank() }?.let { emit(AgentEvent.Thinking(it)) }

            val action = parsed.action
            if (parsed.finalAnswer != null) {
                emit(AgentEvent.FinalAnswer(parsed.finalAnswer))
                return@flow
            }

            if (action != null) {
                emit(AgentEvent.ToolCall(action.toolName, action.args))
                val tool = toolRegistry.find(action.toolName)

                val result = if (tool == null) {
                    ToolResult(
                        success = false,
                        content = "Инструмент '${action.toolName}' не найден. Доступные: " +
                            toolRegistry.all().joinToString(", ") { it.definition.name }
                    )
                } else {
                    try {
                        tool.execute(action.args)
                    } catch (e: Exception) {
                        ToolResult(success = false, content = "Ошибка инструмента: ${e.message}")
                    }
                }

                emit(AgentEvent.ToolResult(action.toolName, result.success, result.content))

                scratchpad.append("Thought: ${parsed.thought.orEmpty()}\n")
                scratchpad.append("Action: ${action.toolName}\n")
                scratchpad.append("Action Input: ${action.args}\n")
                scratchpad.append("Observation: ${result.content}\n")
            }
        }

        // Лимит шагов исчерпан — модель не дала Final Answer.
        emit(AgentEvent.Error("Агент не завершил задачу за $maxSteps шагов. Попробуйте переформулировать запрос."))
    }

    private fun buildPrompt(task: String, scratchpad: String, persona: AgentPersona, activeProject: com.erekeai.domain.project.Project?): String {
        val toolsDescription = toolRegistry.describeForPrompt()
        val personaPreamble = when (persona) {
            AgentPersona.GENERAL -> "Ты — AI Agent внутри приложения ErekeAI."
            AgentPersona.DEVELOPER -> DEVELOPER_PERSONA_PREAMBLE
        }
        val projectBlock = if (activeProject != null) {
            "Активный проект: ${activeProject.name} (${activeProject.description}). " +
                if (activeProject.repoOwner != null) "Репозиторий по умолчанию: ${activeProject.repoOwner}/${activeProject.repoName} (ветка ${activeProject.defaultBranch}), провайдер: ${activeProject.repoProvider}."
                else "Репозиторий не привязан."
        } else "Активный проект не выбран (используйте create_project / set_active_project при необходимости)."
        return """
            $personaPreamble Реши задачу пользователя, при необходимости
            пользуясь инструментами. Строго следуй одному из двух форматов ответа, без лишнего текста:

            Вариант 1 (нужен инструмент):
            Thought: <краткое рассуждение>
            Action: <точное имя инструмента из списка ниже>
            Action Input: <JSON-объект с аргументами, например {"query": "..."}>

            Вариант 2 (задача решена):
            Thought: <краткое рассуждение>
            Final Answer: <окончательный ответ пользователю на русском языке>

            Доступные инструменты:
            $toolsDescription

            Контекст проекта: $projectBlock

            Задача пользователя: $task

            История рассуждений и наблюдений в этой сессии:
            ${scratchpad.ifBlank { "(пока пусто — это первый шаг)" }}

            Дай следующий шаг строго в одном из двух форматов выше.
        """.trimIndent()
    }

    private companion object {
        /**
         * Системная преамбула для персоны 🤖 AI Developer Agent. Явно перечисляет зоны
         * ответственности агента-разработчика и напоминает про безопасный порядок действий
         * (сначала анализ/чтение, затем изменение файлов, затем проверка результата).
         */
        const val DEVELOPER_PERSONA_PREAMBLE = """Ты — 🤖 AI Developer Agent внутри приложения ErekeAI: агент-помощник разработчика.
            Твои зоны ответственности: анализ проекта и структуры кода (analyze_project, code_search, list_files, read_file),
            генерация нового кода и его сохранение (write_file), исправление ошибок и рефакторинг существующего кода,
            запуск проверок и разбор логов (run_static_checks, analyze_log), работа с Git/GitHub (github_action),
            планирование разработки (create_dev_plan). Перед изменением файла сначала прочитай его (read_file),
            после изменения — предложи запустить run_static_checks. Для расплывчатых задач сначала составь
            короткий план (create_dev_plan), затем выполняй его по шагам."""
    }
}
