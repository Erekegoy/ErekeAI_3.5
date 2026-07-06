package com.erekeai.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erekeai.domain.model.AiProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "erekeai_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TERMINAL_ENABLED = booleanPreferencesKey("terminal_agent_enabled")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val ACTIVE_PROJECT_ID = longPreferencesKey("active_project_id")
        val AI_ROUTER_ENABLED = booleanPreferencesKey("ai_router_enabled")
    }

    val selectedProvider: Flow<AiProviderType> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.SELECTED_PROVIDER] ?: AiProviderType.OFFLINE.id

        AiProviderType.entries.firstOrNull { it.id == id }
    ?: AiProviderType.OFFLINE
    }

    suspend fun setSelectedProvider(type: AiProviderType) {
        context.dataStore.edit { it[Keys.SELECTED_PROVIDER] = type.id }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    /**
     * "Настоящий Terminal Agent" по умолчанию ВЫКЛЮЧЕН. [com.erekeai.data.tools.dev.TerminalAgentTool]
     * отказывается выполнять что-либо, пока пользователь явно не включит этот тумблер — это и есть
     * "разрешение", о котором просил пользователь, а не разовое подтверждение на каждую команду.
     */
    val isTerminalEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.TERMINAL_ENABLED] ?: false }

    suspend fun setTerminalEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TERMINAL_ENABLED] = enabled }
    }

    /** "Ollama Manager": адрес сервера (например http://192.168.1.50:11434) и имя используемой модели. */
    val ollamaBaseUrl: Flow<String> = context.dataStore.data.map { it[Keys.OLLAMA_BASE_URL] ?: "http://127.0.0.1:11434" }
    val ollamaModel: Flow<String> = context.dataStore.data.map { it[Keys.OLLAMA_MODEL] ?: "llama3.2" }

    suspend fun setOllamaConfig(baseUrl: String, model: String) {
        context.dataStore.edit { it[Keys.OLLAMA_BASE_URL] = baseUrl; it[Keys.OLLAMA_MODEL] = model }
    }

    /** 🟡 "Project Manager": id активного проекта, null если ни один не выбран. */
    val activeProjectId: Flow<Long?> = context.dataStore.data.map { it[Keys.ACTIVE_PROJECT_ID] }

    suspend fun setActiveProjectId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.ACTIVE_PROJECT_ID) else prefs[Keys.ACTIVE_PROJECT_ID] = id
        }
    }

    /** 🟡 "AI Router": включает автоматический выбор провайдера по задаче вместо ручного выбора. */
    val isAiRouterEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_ROUTER_ENABLED] ?: false }

    suspend fun setAiRouterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_ROUTER_ENABLED] = enabled }
    }
}
