package com.erekeai.features.chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.R
import com.erekeai.domain.model.Role
import com.erekeai.features.chat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenDevAgent: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.chat_title)) },
                    actions = {
                        IconButton(onClick = onOpenDevAgent) {
                            Icon(Icons.Default.SmartToy, contentDescription = "AI Developer Agent")
                        }
                        IconButton(onClick = onOpenKnowledgeBase) {
                            Icon(Icons.Default.MenuBook, contentDescription = "База знаний")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                )
                AgentModeBar(
                    enabled = state.agentModeEnabled,
                    onToggle = viewModel::onToggleAgentMode
                )
            }
        },
        bottomBar = {
            Column {
                state.agentThinking?.let { thinking ->
                    Text(
                        text = "💭 $thinking",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                ChatInputBar(
                    value = state.input,
                    enabled = !state.isSending,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(state.messages) { message ->
                when (message.role) {
                    Role.TOOL -> ToolStepBubble(text = message.text)
                    else -> MessageBubble(text = message.text, isUser = message.role == Role.USER)
                }
            }
        }
    }
}

@Composable
private fun AgentModeBar(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Агент (веб-поиск, файлы, калькулятор)", style = MaterialTheme.typography.labelMedium)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun MessageBubble(text: String, isUser: Boolean) {
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = text, modifier = Modifier.padding(12.dp))
        }
    }
}

/** Компактное отображение технического шага агента (вызов инструмента / его результат). */
@Composable
private fun ToolStepBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(start = 4.dp, end = 32.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Спросите что-нибудь…") },
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}
