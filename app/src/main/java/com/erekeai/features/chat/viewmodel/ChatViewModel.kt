package com.erekeai.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.ChatMessage
import com.erekeai.domain.repository.ChatRepository
import com.erekeai.domain.usecase.RunAgentTaskUseCase
import com.erekeai.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val selectedProvider: AiProviderType = AiProviderType.OFFLINE,
    /** Режим агента: модель может рассуждать и вызывать инструменты (веб-поиск, файлы, калькулятор). */
    val agentModeEnabled: Boolean = false,
    /** Текущее "рассуждение" агента, показываемое как временный индикатор, пока не сохранён финальный ответ. */
    val agentThinking: String? = null,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val runAgentTaskUseCase: RunAgentTaskUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var conversationId: Long = 0L

    init {
        viewModelScope.launch {
            conversationId = chatRepository.createConversation("Новый диалог")
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

    fun onToggleAgentMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(agentModeEnabled = enabled)
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) return
        _uiState.value = _uiState.value.copy(input = "", isSending = true, error = null, agentThinking = null)

        if (_uiState.value.agentModeEnabled) {
            runAsAgent(text)
        } else {
            runAsPlainChat(text)
        }
    }

    private fun runAsPlainChat(text: String) {
        viewModelScope.launch {
            try {
                sendMessageUseCase(
                    conversationId = conversationId,
                    text = text,
                    provider = _uiState.value.selectedProvider
                ).collect { /* фрагменты уже сохраняются и придут через observeMessages */ }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Неизвестная ошибка")
            } finally {
                _uiState.value = _uiState.value.copy(isSending = false)
            }
        }
    }

    private fun runAsAgent(text: String) {
        viewModelScope.launch {
            try {
                runAgentTaskUseCase(
                    conversationId = conversationId,
                    text = text,
                    provider = _uiState.value.selectedProvider
                ).collect { event ->
                    // Шаги (ToolCall/ToolResult/FinalAnswer) уже сохраняются use case'ом
                    // и придут через observeMessages; здесь только показываем "живой" индикатор мышления.
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
