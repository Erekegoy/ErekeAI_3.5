package com.erekeai.features.devagent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.domain.model.Role
import com.erekeai.features.devagent.viewmodel.DevAgentViewModel

/**
 * Экран 🤖 AI Developer Agent. Панель быстрых действий покрывает основные сценарии
 * агента-разработчика: анализ проекта, генерация кода, исправление ошибок/рефакторинг,
 * запуск проверок, разбор логов, работа с Git/GitHub, планирование разработки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevAgentScreen(
    onBack: () -> Unit,
    viewModel: DevAgentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 AI Developer Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
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
                state.error?.let { error ->
                    Text(
                        text = "⚠️ $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                DevInputBar(
                    value = state.input,
                    enabled = !state.isSending,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            QuickActionsRow(
                onAnalyzeProject = {
                    viewModel.runQuickAction(
                        "Проанализируй структуру проекта (analyze_project, list_files) и опиши " +
                            "архитектуру, основные модули и возможные проблемы."
                    )
                },
                onGenerateCode = {
                    viewModel.fillTemplate("Сгенерируй код: ")
                },
                onFixAndRefactor = {
                    viewModel.fillTemplate("Найди и исправь ошибку / отрефактори следующий файл: ")
                },
                onRunChecks = {
                    viewModel.fillTemplate("Запусти проверки (run_static_checks) для файла: ")
                },
                onAnalyzeLog = {
                    viewModel.fillTemplate("Проанализируй лог/стектрейс и предложи причину и исправление:\n")
                },
                onGitHub = {
                    viewModel.fillTemplate("Через github_action ")
                },
                onPlanning = {
                    viewModel.fillTemplate("Составь план разработки (create_dev_plan) для задачи: ")
                }
            )
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item { DevAgentIntro() }
                }
                items(state.messages) { message ->
                    when (message.role) {
                        Role.TOOL -> DevToolStepBubble(text = message.text)
                        else -> DevMessageBubble(text = message.text, isUser = message.role == Role.USER)
                    }
                }
            }
        }
    }
}

@Composable
private fun DevAgentIntro() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Привет! Я 🤖 AI Developer Agent.", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Помогу проанализировать проект, написать или исправить код, запустить проверки, " +
                    "разобрать лог ошибки, поработать с Git/GitHub и спланировать задачу. " +
                    "Выберите действие выше или опишите задачу своими словами.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickActionsRow(
    onAnalyzeProject: () -> Unit,
    onGenerateCode: () -> Unit,
    onFixAndRefactor: () -> Unit,
    onRunChecks: () -> Unit,
    onAnalyzeLog: () -> Unit,
    onGitHub: () -> Unit,
    onPlanning: () -> Unit
) {
    val actions = listOf(
        QuickAction("Анализ проекта", Icons.Default.FindInPage, onAnalyzeProject),
        QuickAction("Генерация кода", Icons.Default.Code, onGenerateCode),
        QuickAction("Исправить/рефактор", Icons.Default.BugReport, onFixAndRefactor),
        QuickAction("Проверки", Icons.Default.PlaylistAddCheck, onRunChecks),
        QuickAction("Анализ логов", Icons.Default.Terminal, onAnalyzeLog),
        QuickAction("Git/GitHub", Icons.Default.AccountTree, onGitHub),
        QuickAction("Планирование", Icons.Default.EventNote, onPlanning)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            AssistChip(
                onClick = action.onClick,
                label = { Text(action.label) },
                leadingIcon = { Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
private fun DevMessageBubble(text: String, isUser: Boolean) {
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
private fun DevToolStepBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
private fun DevInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Опишите задачу разработки…") },
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}
