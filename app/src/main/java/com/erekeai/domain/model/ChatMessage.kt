package com.erekeai.domain.model

/**
 * Роль автора сообщения в диалоге.
 */
enum class Role {
    USER,
    ASSISTANT,
    SYSTEM,
    /** Технический шаг агента: вызов инструмента и его результат (для отображения в UI). */
    TOOL
}

/**
 * Доменная модель одного сообщения чата.
 */
data class ChatMessage(
    val id: Long = 0L,
    val conversationId: Long,
    val role: Role,
    val text: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val providerId: String? = null
)

/**
 * Диалог (переписка), группирующая сообщения.
 */
data class Conversation(
    val id: Long = 0L,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
