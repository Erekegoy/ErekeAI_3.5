package com.erekeai.features.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erekeai.core.security.SecureKeyStore
import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.mcp.McpServerConfig
import com.erekeai.domain.mcp.McpServerStore
import com.erekeai.domain.model.AiProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val selectedProvider: AiProviderType = AiProviderType.GEMINI,
    val apiKeys: Map<AiProviderType, String> = emptyMap(),
    /** Personal Access Token для инструмента github_action (🤖 AI Developer Agent). Необязателен для чтения публичных репозиториев. */
    val githubToken: String = "",
    /** ⚠️ Настоящий Terminal Agent выключен по умолчанию — пользователь должен явно разрешить. */
    val terminalEnabled: Boolean = false,
    val ollamaBaseUrl: String = "http://127.0.0.1:11434",
    val ollamaModel: String = "llama3.2",
    val cloudSyncUrl: String = "",
    val cloudSyncToken: String = "",
    val mcpServers: List<McpServerConfig> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val keyStore: SecureKeyStore,
    private val mcpServerStore: McpServerStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiKeys = AiProviderType.entries.associateWith { keyStore.getKey(it.id).orEmpty() },
            githubToken = keyStore.getKey(GITHUB_KEY_ID).orEmpty(),
            cloudSyncUrl = keyStore.getKey(CLOUD_SYNC_URL_KEY_ID).orEmpty(),
            cloudSyncToken = keyStore.getKey(CLOUD_SYNC_TOKEN_KEY_ID).orEmpty()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsDataStore.selectedProvider.collect { provider ->
                _uiState.value = _uiState.value.copy(selectedProvider = provider)
            }
        }
        viewModelScope.launch {
            combine(settingsDataStore.isTerminalEnabled, settingsDataStore.ollamaBaseUrl, settingsDataStore.ollamaModel) { enabled, url, model ->
                Triple(enabled, url, model)
            }.collect { (enabled, url, model) ->
                _uiState.value = _uiState.value.copy(terminalEnabled = enabled, ollamaBaseUrl = url, ollamaModel = model)
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(mcpServers = mcpServerStore.getServers())
        }
    }

    fun selectProvider(type: AiProviderType) {
        viewModelScope.launch { settingsDataStore.setSelectedProvider(type) }
    }

    fun saveApiKey(type: AiProviderType, key: String) {
        keyStore.saveKey(type.id, key)
        _uiState.value = _uiState.value.copy(apiKeys = _uiState.value.apiKeys.toMutableMap().apply { put(type, key) })
    }

    /** Сохраняет GitHub Personal Access Token, которым пользуется github_action, git_ops, build_apk_agent. */
    fun saveGithubToken(token: String) {
        keyStore.saveKey(GITHUB_KEY_ID, token)
        _uiState.value = _uiState.value.copy(githubToken = token)
    }

    /** "Настоящий Terminal Agent" — явное разрешение пользователя, выключено по умолчанию. */
    fun setTerminalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTerminalEnabled(enabled)
            _uiState.value = _uiState.value.copy(terminalEnabled = enabled)
        }
    }

    fun saveOllamaConfig(baseUrl: String, model: String) {
        viewModelScope.launch {
            settingsDataStore.setOllamaConfig(baseUrl, model)
            _uiState.value = _uiState.value.copy(ollamaBaseUrl = baseUrl, ollamaModel = model)
        }
    }

    fun saveCloudSyncConfig(url: String, token: String) {
        keyStore.saveKey(CLOUD_SYNC_URL_KEY_ID, url)
        keyStore.saveKey(CLOUD_SYNC_TOKEN_KEY_ID, token)
        _uiState.value = _uiState.value.copy(cloudSyncUrl = url, cloudSyncToken = token)
    }

    fun addMcpServer(id: String, name: String, url: String, token: String) {
        viewModelScope.launch {
            mcpServerStore.saveServer(McpServerConfig(id, name, url, token.ifBlank { null }))
            _uiState.value = _uiState.value.copy(mcpServers = mcpServerStore.getServers())
        }
    }

    fun deleteMcpServer(id: String) {
        viewModelScope.launch {
            mcpServerStore.deleteServer(id)
            _uiState.value = _uiState.value.copy(mcpServers = mcpServerStore.getServers())
        }
    }

    private companion object {
        const val GITHUB_KEY_ID = "github"
        const val CLOUD_SYNC_URL_KEY_ID = "cloud_sync_url"
        const val CLOUD_SYNC_TOKEN_KEY_ID = "cloud_sync_token"
    }
}
