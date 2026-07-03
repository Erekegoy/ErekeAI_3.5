package com.erekeai.data.scheduler

import kotlinx.coroutines.flow.first
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erekeai.app.R
import com.erekeai.data.local.db.ScheduledTaskDao
import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.Role
import com.erekeai.domain.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker, который реально выполняет фоновую задачу: находит/создаёт диалог "🕒 Фоновые задачи",
 * прогоняет текст задачи через [AgentOrchestrator] (с инструментами — полноценный фоновый агент,
 * а не просто напоминание), сохраняет результат в историю и показывает уведомление.
 */
@HiltWorker
class BackgroundAgentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduledTaskDao: ScheduledTaskDao,
    private val chatRepository: ChatRepository,
    private val agentOrchestrator: AgentOrchestrator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId == -1L) return Result.failure()
        val task = scheduledTaskDao.getById(taskId) ?: return Result.failure()
        if (!task.enabled) return Result.success()

        val provider = AiProviderType.entries.firstOrNull { it.id == task.providerId } ?: AiProviderType.GEMINI
        val conversationId = findOrCreateBackgroundConversation()

        chatRepository.appendMessage(conversationId, Role.USER, "🕒 [Фоновая задача: ${task.name}] ${task.taskText}", provider.id)
        val history = chatRepository.getHistorySnapshot(conversationId)

        var finalText = ""
        try {
            agentOrchestrator.run(history, provider, persona = AgentPersona.GENERAL).collect { event ->
                when (event) {
                    is AgentEvent.FinalAnswer -> { finalText = event.text; chatRepository.appendMessage(conversationId, Role.ASSISTANT, event.text, provider.id) }
                    is AgentEvent.Error -> { finalText = event.message; chatRepository.appendMessage(conversationId, Role.SYSTEM, event.message, provider.id) }
                    is AgentEvent.ToolResult -> chatRepository.appendMessage(conversationId, Role.TOOL, "${if (event.success) "✅" else "⚠️"} ${event.content}", provider.id)
                    else -> {}
                }
            }
        } catch (e: Exception) {
            finalText = "Ошибка фонового агента: ${e.message}"
        }

        scheduledTaskDao.setLastRun(taskId, System.currentTimeMillis())
        notifyResult(task.name, finalText)
        return Result.success()
    }

    private suspend fun findOrCreateBackgroundConversation(): Long {
    val conversations = chatRepository.observeConversations().first()

    val existing = conversations.firstOrNull {
        it.title == BACKGROUND_CONVERSATION_TITLE
    }

    return existing?.id
        ?: chatRepository.createConversation(BACKGROUND_CONVERSATION_TITLE)
}

    private fun notifyResult(taskName: String, result: String) {
        val context = applicationContext
        val channelId = "ereke_background_agent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(channelId, "ErekeAI: фоновые задачи", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Фоновая задача выполнена: $taskName")
            .setContentText(result.take(150))
            .setStyle(NotificationCompat.BigTextStyle().bigText(result.take(1000)))
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(taskName.hashCode(), notification)
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val BACKGROUND_CONVERSATION_TITLE = "🕒 Фоновые задачи"
    }
}
