package com.erekeai.features.devagent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.repository.ChatRepository
import com.erekeai.domain.usecase.RunAgentTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🤖 AI Developer Agent — отдельный диалог, всегда работающий в режиме агента (Tool Calling)
 * с персоной [AgentPersona.DEVELOPER]. Экран предлагает быстрые действия под основные сценарии:
 * анализ проекта, генерация кода, исправление ошибок/рефакторинг, проверки и разбор логов,
 * работа с Git/GitHub, планирование разработки.
 */
data class DevAgentUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val selectedProvider: AiProviderType = AiProviderType.GEMINI,
    val agentThinking: String? = null,
    val error: String? = null
)

@HiltViewModel
class DevAgentViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val runAgentTaskUseCase: RunAgentTaskUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevAgentUiState())
    val uiState: StateFlow<DevAgentUiState> = _uiState

    private var conversationId: Long = 0L

    init {
        viewModelScope.launch {
            conversationId = chatRepository.createConversation("🤖 AI Developer Agent")
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        viewModelScope.launch {
            settingsDataStore.selectedProvider.collect { provider ->
                _uiState.value = _uiState.value.copy(selectedProvider = provider)
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(input = text)
    }

    /** Заполняет поле ввода шаблоном быстрого действия, оставляя пользователю уточнить детали. */
    fun fillTemplate(template: String) {
        _uiState.value = _uiState.value.copy(input = template)
    }

    /** Быстрое действие, которое можно отправить сразу — не требует дополнительных деталей от пользователя. */
    fun runQuickAction(prompt: String) {
        send(prompt)
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) return
        send(text)
    }

    private fun send(text: String) {
        _uiState.value = _uiState.value.copy(input = "", isSending = true, error = null, agentThinking = null)
        viewModelScope.launch {
            try {
                runAgentTaskUseCase(
                    conversationId = conversationId,
                    text = text,
                    provider = _uiState.value.selectedProvider,
                    persona = AgentPersona.DEVELOPER
                ).collect { event ->
                    if (event is AgentEvent.Thinking) {
                        _uiState.value = _uiState.value.copy(agentThinking = event.text)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Неизвестная ошибка агента")
            } finally {
                _uiState.value = _uiState.value.copy(isSending = false, agentThinking = null)
            }
        }
    }
}
