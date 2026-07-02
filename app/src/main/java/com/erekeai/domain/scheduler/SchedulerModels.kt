package com.erekeai.domain.scheduler

enum class ScheduleType {
    /** Через N минут один раз (минимум 15 минут — ограничение Android WorkManager). */
    ONCE_AFTER_MINUTES,
    /** Периодически каждые N минут (минимум 15 — ограничение WorkManager). */
    PERIODIC_MINUTES
}

data class ScheduledTask(
    val id: Long = 0L,
    val name: String,
    val taskText: String,
    val providerId: String,
    val scheduleType: ScheduleType,
    val intervalMinutes: Int,
    val enabled: Boolean = true,
    val lastRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
