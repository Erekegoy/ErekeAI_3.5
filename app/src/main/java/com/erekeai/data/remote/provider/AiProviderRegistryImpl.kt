package com.erekeai.data.remote.provider

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.repository.AiProvider
import com.erekeai.domain.repository.AiProviderRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderRegistryImpl @Inject constructor(
    private val gemini: GeminiProvider,
    private val groq: GroqProvider,
    private val openAi: OpenAiProvider,
    private val offline: OfflineProvider,
    private val ollama: OllamaProvider,
    private val settingsDataStore: SettingsDataStore
) : AiProviderRegistry {

    private val providers: Map<AiProviderType, AiProvider> = mapOf(
        AiProviderType.GEMINI to gemini,
        AiProviderType.GROQ to groq,
        AiProviderType.OPENAI to openAi,
        AiProviderType.OFFLINE to offline,
        AiProviderType.OLLAMA to ollama
    )

    override fun getProvider(type: AiProviderType): AiProvider {
        if (type == AiProviderType.OLLAMA) {
            // Подтягиваем актуальные настройки (адрес сервера/модель) перед каждым использованием,
            // чтобы изменения в Настройках применялись без пересоздания провайдера.
            runBlocking {
                val url = settingsDataStore.ollamaBaseUrl.first()
                val model = settingsDataStore.ollamaModel.first()
                ollama.configure(url, model)
            }
        }
        return providers[type] ?: error("Провайдер $type не зарегистрирован")
    }

    override fun availableProviders(): List<AiProvider> = providers.values.toList()
}
