package com.erekeai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val taskText: String,
    val providerId: String,
    val scheduleType: String,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val lastRunAt: Long?,
    val createdAt: Long,
    /** id уникальной WorkManager-задачи (для отмены/переключения). */
    val workRequestTag: String
)
