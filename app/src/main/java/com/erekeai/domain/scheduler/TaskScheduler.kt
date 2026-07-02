package com.erekeai.domain.scheduler

/**
 * 🟡 "Планировщик длительных задач и фоновых агентов" — поверх Android WorkManager (переживает
 * перезапуск приложения и перезагрузку устройства). ЧЕСТНО про ограничение платформы: минимальный
 * интервал периодической задачи в WorkManager — 15 минут (это ограничение Android, не наше); для
 * задачи "раз в минуту" придётся использовать внешний сервер (например, дергать [ErekeApiServer]
 * cron'ом с ПК) — на голом Android чаще 15 минут фоновые задачи не предназначены.
 */
interface TaskScheduler {
    suspend fun schedule(
        name: String,
        taskText: String,
        providerId: String,
        scheduleType: ScheduleType,
        intervalMinutes: Int
    ): Long

    suspend fun listTasks(): List<ScheduledTask>
    suspend fun cancel(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
