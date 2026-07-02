package com.erekeai.data.scheduler

import android.content.Context
import androidx.work.*
import com.erekeai.data.local.db.ScheduledTaskDao
import com.erekeai.data.local.db.ScheduledTaskEntity
import com.erekeai.domain.scheduler.ScheduleType
import com.erekeai.domain.scheduler.ScheduledTask
import com.erekeai.domain.scheduler.TaskScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScheduledTaskDao
) : TaskScheduler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override suspend fun schedule(
        name: String,
        taskText: String,
        providerId: String,
        scheduleType: ScheduleType,
        intervalMinutes: Int
    ): Long = withContext(Dispatchers.IO) {
        // WorkManager: минимум 15 минут для периодических задач — ограничение платформы Android.
        val effectiveInterval = if (scheduleType == ScheduleType.PERIODIC_MINUTES) maxOf(intervalMinutes, 15) else intervalMinutes
        val tag = "ereke_scheduled_${System.currentTimeMillis()}"

        val id = dao.insert(
            ScheduledTaskEntity(
                name = name, taskText = taskText, providerId = providerId,
                scheduleType = scheduleType.name, intervalMinutes = effectiveInterval, enabled = true,
                lastRunAt = null, createdAt = System.currentTimeMillis(), workRequestTag = tag
            )
        )

        val inputData = Data.Builder().putLong(BackgroundAgentWorker.KEY_TASK_ID, id).build()

        when (scheduleType) {
            ScheduleType.ONCE_AFTER_MINUTES -> {
                val request = OneTimeWorkRequestBuilder<BackgroundAgentWorker>()
                    .setInitialDelay(effectiveInterval.toLong(), TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .addTag(tag)
                    .build()
                workManager.enqueue(request)
            }
            ScheduleType.PERIODIC_MINUTES -> {
                val request = PeriodicWorkRequestBuilder<BackgroundAgentWorker>(effectiveInterval.toLong(), TimeUnit.MINUTES)
                    .setInputData(inputData)
                    .addTag(tag)
                    .build()
                workManager.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, request)
            }
        }
        id
    }

    override suspend fun listTasks(): List<ScheduledTask> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { workManager.cancelAllWorkByTag(it.workRequestTag) }
        dao.delete(id)
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.setEnabled(id, enabled)
        if (!enabled) dao.getById(id)?.let { workManager.cancelAllWorkByTag(it.workRequestTag) }
    }

    private fun ScheduledTaskEntity.toDomain() = ScheduledTask(
        id = id, name = name, taskText = taskText, providerId = providerId,
        scheduleType = ScheduleType.valueOf(scheduleType), intervalMinutes = intervalMinutes,
        enabled = enabled, lastRunAt = lastRunAt, createdAt = createdAt
    )
}
