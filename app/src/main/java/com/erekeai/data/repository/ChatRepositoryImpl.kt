package com.erekeai.data.repository

import com.erekeai.data.local.db.ChatDao
import com.erekeai.data.local.db.ConversationEntity
import com.erekeai.data.local.db.MessageEntity
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.model.Conversation
import com.erekeai.domain.model.Role
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatDao,
    private val providerRegistry: AiProviderRegistry
) : ChatRepository {

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    override fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list -> list.map { it.toDomain() } }

    override suspend fun createConversation(title: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertConversation(ConversationEntity(title = title, createdAt = now, updatedAt = now))
    }

    override suspend fun sendMessage(
        conversationId: Long,
        text: String,
        provider: AiProviderType,
        imageUri: String?
    ): Flow<String> {
        // Сохраняем сообщение пользователя
        dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = Role.USER.name,
                text = text,
                imageUri = imageUri,
                timestamp = System.currentTimeMillis(),
                providerId = provider.id
            )
        )
        dao.touchConversation(conversationId, System.currentTimeMillis())

        val history = dao.observeMessages(conversationId).first().map { it.toDomain() }
        val aiProvider = providerRegistry.getProvider(provider)

        return flow {
            val builder = StringBuilder()
            aiProvider.streamReply(history).collect { chunk ->
                builder.append(chunk)
                emit(chunk)
            }
            // По завершении стрима сохраняем полный ответ ассистента
            dao.insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = Role.ASSISTANT.name,
                    text = builder.toString(),
                    imageUri = null,
                    timestamp = System.currentTimeMillis(),
                    providerId = provider.id
                )
            )
            dao.touchConversation(conversationId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        dao.deleteMessagesForConversation(conversationId)
        dao.deleteConversation(conversationId)
    }

    override suspend fun getHistorySnapshot(conversationId: Long): List<ChatMessage> =
        dao.observeMessages(conversationId).first().map { it.toDomain() }

    override suspend fun appendMessage(
        conversationId: Long,
        role: Role,
        text: String,
        providerId: String?
    ): Long {
        val id = dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role.name,
                text = text,
                imageUri = null,
                timestamp = System.currentTimeMillis(),
                providerId = providerId
            )
        )
        dao.touchConversation(conversationId, System.currentTimeMillis())
        return id
    }

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = Role.valueOf(role),
        text = text,
        imageUri = imageUri,
        timestamp = timestamp,
        providerId = providerId
    )

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
