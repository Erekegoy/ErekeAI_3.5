package com.erekeai.domain.planner

import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow

/**
 * ✅ "Multi-Agent Planner" — декомпозирует сложную задачу пользователя на подзадачи (план),
 * затем прогоняет каждую подзадачу через отдельный запуск [com.erekeai.domain.agent.AgentOrchestrator]
 * (то есть каждая подзадача — это, по сути, отдельный "суб-агент" с собственным ReAct-циклом
 * и своим бюджетом шагов), а в конце сводит результаты всех подзадач в единый итоговый отчёт.
 *
 * Это НЕ параллельные независимые агенты (на телефоне последовательное выполнение надёжнее и
 * прозрачнее для пользователя — видно, что именно и в каком порядке происходит), а
 * "planner → executor(N) → aggregator" пайплайн, что и является практическим смыслом
 * multi-agent систем для конечного пользователя.
 */
interface MultiAgentPlanner {
    fun run(
        goal: String,
        provider: AiProviderType,
        persona: AgentPersona = AgentPersona.GENERAL,
        maxSubTasks: Int = 5
    ): Flow<PlannerEvent>
}
