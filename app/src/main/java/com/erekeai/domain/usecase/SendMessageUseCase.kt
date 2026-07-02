package com.erekeai.domain.usecase

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.repository.ChatRepository
import com.erekeai.domain.router.AiRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Инкапсулирует бизнес-логику отправки сообщения пользователем и получения
 * потокового ответа от выбранного (или, если включён 🟡 AI Router, автоматически
 * подобранного) AI-провайдера.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiRouter: AiRouter,
    private val providerRegistry: AiProviderRegistry,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(
        conversationId: Long,
        text: String,
        provider: AiProviderType,
        imageUri: String? = null
    ): Flow<String> {
        require(text.isNotBlank() || imageUri != null) { "Сообщение не может быть пустым" }
        val resolvedProvider = resolveProvider(text, imageUri != null, provider)
        return chatRepository.sendMessage(conversationId, text, resolvedProvider, imageUri)
    }

    private suspend fun resolveProvider(text: String, hasImage: Boolean, manualProvider: AiProviderType): AiProviderType {
        if (!settingsDataStore.isAiRouterEnabled.first()) return manualProvider
        val available = providerRegistry.availableProviders().filter { it.isConfigured() }.map { it.type }
        if (available.isEmpty()) return manualProvider
        return aiRouter.route(text, hasImage, available).provider
    }
}
