package com.erekeai.data.tools

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.scheduler.ScheduleType
import com.erekeai.domain.scheduler.TaskScheduler
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleTaskTool @Inject constructor(private val scheduler: TaskScheduler) : Tool {
    override val definition = ToolDefinition(
        name = "schedule_task",
        description = "Планирует выполнение задачи агентом в фоне (один раз через N минут или периодически). " +
            "ВАЖНО: минимальный интервал в Android — 15 минут (ограничение системы).",
        parameters = listOf(
            ToolParameter("name", "короткое название задачи"),
            ToolParameter("task_text", "текст задачи, который будет выполнен агентом"),
            ToolParameter("schedule_type", "once | periodic"),
            ToolParameter("interval_minutes", "через сколько минут выполнить / период повтора (минимум 15 для periodic)"),
            ToolParameter("provider", "gemini | groq | openai | ollama (по умолчанию gemini)", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val name = args["name"]?.trim() ?: return ToolResult(false, "Не указан 'name'")
        val taskText = args["task_text"]?.trim() ?: return ToolResult(false, "Не указан 'task_text'")
        val interval = args["interval_minutes"]?.toIntOrNull() ?: return ToolResult(false, "Не указан или некорректен 'interval_minutes'")
        val scheduleType = when (args["schedule_type"]?.trim()) {
            "once" -> ScheduleType.ONCE_AFTER_MINUTES
            "periodic" -> ScheduleType.PERIODIC_MINUTES
            else -> return ToolResult(false, "Не указан или неизвестен 'schedule_type'. Доступны: once, periodic")
        }
        val providerId = args["provider"]?.trim() ?: AiProviderType.GEMINI.id

        val id = scheduler.schedule(name, taskText, providerId, scheduleType, interval)
        return ToolResult(true, "Задача '$name' запланирована (id=$id). Результат появится в диалоге '🕒 Фоновые задачи' и в уведомлении.")
    }
}

@Singleton
class ListScheduledTasksTool @Inject constructor(private val scheduler: TaskScheduler) : Tool {
    override val definition = ToolDefinition("list_scheduled_tasks", "Показывает список запланированных фоновых задач", emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val tasks = scheduler.listTasks()
        if (tasks.isEmpty()) return ToolResult(true, "Запланированных задач нет.")
        return ToolResult(true, tasks.joinToString("\n") { t ->
            "${if (t.enabled) "🟢" else "⚪"} id=${t.id} '${t.name}' (${t.scheduleType}, ${t.intervalMinutes} мин), последний запуск: ${t.lastRunAt?.let { java.util.Date(it) } ?: "ещё не выполнялась"}"
        })
    }
}

@Singleton
class CancelScheduledTaskTool @Inject constructor(private val scheduler: TaskScheduler) : Tool {
    override val definition = ToolDefinition("cancel_scheduled_task", "Отменяет запланированную задачу по id", listOf(ToolParameter("task_id", "id задачи")))
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val id = args["task_id"]?.toLongOrNull() ?: return ToolResult(false, "Не указан или некорректен 'task_id'")
        scheduler.cancel(id)
        return ToolResult(true, "Задача id=$id отменена")
    }
}
