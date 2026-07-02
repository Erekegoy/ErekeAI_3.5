package com.erekeai.data.remote.provider

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.repository.AiProvider
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Заглушка офлайн-провайдера. В будущем здесь будет интеграция локальной
 * on-device модели (например, через MediaPipe LLM Inference / GGUF).
 * Пока — простой отладочный ответ без обращения к сети.
 */
@Singleton
class OfflineProvider @Inject constructor() : AiProvider {

    override val type = AiProviderType.OFFLINE

    override fun isConfigured(): Boolean = false // помечаем как "в разработке"

    override fun streamReply(history: List<ChatMessage>) = flow {
        emit("Offline AI пока в разработке. Выберите другого провайдера в настройках.")
    }
}
