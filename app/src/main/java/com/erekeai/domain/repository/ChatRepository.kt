package com.erekeai.domain.repository

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>

    fun observeConversations(): Flow<List<Conversation>>

    suspend fun createConversation(title: String): Long

    suspend fun sendMessage(
        conversationId: Long,
        text: String,
        provider: AiProviderType,
        imageUri: String? = null
    ): Flow<String>

    suspend fun deleteConversation(conversationId: Long)

    /** Текущий снимок истории диалога (используется агентом для построения промпта). */
    suspend fun getHistorySnapshot(conversationId: Long): List<ChatMessage>

    /** Сохраняет произвольное сообщение (например, шаг агента с ролью TOOL) без обращения к AI-провайдеру. */
    suspend fun appendMessage(
        conversationId: Long,
        role: com.erekeai.domain.model.Role,
        text: String,
        providerId: String? = null
    ): Long
}
