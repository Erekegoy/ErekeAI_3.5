package com.erekeai.domain.repository

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Общий контракт для всех AI-провайдеров (Gemini, Groq, OpenAI, Offline и т.д.).
 * Каждый провайдер реализует свою логику вызова API, но предоставляет единый интерфейс
 * для остальной части приложения — это и есть "AI Provider System" из README.
 */
interface AiProvider {

    val type: AiProviderType

    /** Готов ли провайдер к работе (например, задан ли API-ключ). */
    fun isConfigured(): Boolean

    /**
     * Отправляет историю сообщений и стримит ответ по частям (потоковая генерация).
     * Каждый элемент Flow — это очередной фрагмент текста ответа.
     */
    fun streamReply(history: List<ChatMessage>): Flow<String>
}

/**
 * Реестр провайдеров: позволяет получить нужный провайдер по типу и
 * узнать, какие из них сконфигурированы и доступны пользователю.
 */
interface AiProviderRegistry {
    fun getProvider(type: AiProviderType): AiProvider
    fun availableProviders(): List<AiProvider>
}
