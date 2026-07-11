package com.erekeai.features.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.domain.model.AiProviderType
import com.erekeai.llm.ModelInfo
import com.erekeai.features.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Активный AI-провайдер", style = MaterialTheme.typography.titleMedium)
            }
            items(AiProviderType.entries.toList()) { provider ->
                ProviderRow(
                    provider = provider,
                    selected = provider == state.selectedProvider,
                    apiKey = state.apiKeys[provider].orEmpty(),
                    onSelect = { viewModel.selectProvider(provider) },
                    onKeyChange = { viewModel.saveApiKey(provider, it) },
                    models = state.models,
                    onImportModel = viewModel::importModel,
                    onSetActiveModel = viewModel::setActiveModel,
                    onDeleteModel = viewModel::deleteModel,
                    onRenameModel = viewModel::renameModel
                )
            }
            item {
                Text("🤖 AI Developer Agent — Git/GitHub", style = MaterialTheme.typography.titleMedium)
            }
            item {
                GitHubTokenCard(
                    token = state.githubToken,
                    onTokenChange = viewModel::saveGithubToken
                )
            }
            item {
                Text("⚠️ Разработка / автоматизация", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Разрешить Terminal Agent", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Выполняет ограниченный набор команд (ls/cat/grep и т.п.) ТОЛЬКО внутри песочницы " +
                                        "приложения. Не root-доступ к устройству. Выключено по умолчанию.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(checked = state.terminalEnabled, onCheckedChange = viewModel::setTerminalEnabled)
                        }
                    }
                }
            }
            item {
                Text("🦙 Ollama Manager (локальный LLM)", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OllamaConfigCard(
                    baseUrl = state.ollamaBaseUrl,
                    model = state.ollamaModel,
                    onSave = viewModel::saveOllamaConfig
                )
            }
            item {
                Text("☁️ Cloud Sync", style = MaterialTheme.typography.titleMedium)
            }
            item {
                CloudSyncCard(
                    url = state.cloudSyncUrl,
                    token = state.cloudSyncToken,
                    onSave = viewModel::saveCloudSyncConfig
                )
            }
            item {
                Text("🔌 MCP-серверы", style = MaterialTheme.typography.titleMedium)
            }
            items(state.mcpServers) { server ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(server.name, style = MaterialTheme.typography.titleSmall)
                            Text(server.url, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { viewModel.deleteMcpServer(server.id) }) { Text("Удалить") }
                    }
                }
            }
            item {
                AddMcpServerCard(onAdd = viewModel::addMcpServer)
            }
            item {
    Text(
        "ℹ️ О приложении",
        style = MaterialTheme.typography.titleMedium
    )
}

item {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "🤖 ErekeAI",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Версия: 0.1.0")

            HorizontalDivider()

            Text("Создатель")
            Text(
                text = "Ерлан Б",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            Text("Описание")
            Text(
                "ErekeAI — персональная AI-платформа с поддержкой нескольких моделей ИИ, AI Developer Agent, локальных LLM, MCP, GitHub, автоматизации и расширяемой архитектуры."
            )

            HorizontalDivider()

            Text("© 2026 Ерлан Б. Все права защищены.")
        }
    }
}
        }
    }
}

@Composable
private fun OllamaConfigCard(baseUrl: String, model: String, onSave: (String, String) -> Unit) {
    var localUrl by remember(baseUrl) { mutableStateOf(baseUrl) }
    var localModel by remember(model) { mutableStateOf(model) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Ollama должна быть запущена на ПК/сервере в вашей сети (ollama serve). Инференс на самом " +
                    "телефоне не подключён (см. описание gguf_manager).",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localUrl,
                onValueChange = { localUrl = it; onSave(it, localModel) },
                label = { Text("Адрес сервера, например http://192.168.1.50:11434") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localModel,
                onValueChange = { localModel = it; onSave(localUrl, it) },
                label = { Text("Модель, например llama3.2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CloudSyncCard(url: String, token: String, onSave: (String, String) -> Unit) {
    var localUrl by remember(url) { mutableStateOf(url) }
    var localToken by remember(token) { mutableStateOf(token) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Ваш собственный REST-эндпоинт (Firebase Realtime DB, Supabase, свой сервер) для push/pull через cloud_sync.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localUrl,
                onValueChange = { localUrl = it; onSave(it, localToken) },
                label = { Text("Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localToken,
                onValueChange = { localToken = it; onSave(localUrl, it) },
                label = { Text("Bearer token (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddMcpServerCard(onAdd: (id: String, name: String, url: String, token: String) -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Добавить MCP-сервер", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("id (например 'work')") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL сервера (Streamable HTTP)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Bearer token (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (id.isNotBlank() && url.isNotBlank()) {
                    onAdd(id, name.ifBlank { id }, url, token)
                    id = ""; name = ""; url = ""; token = ""
                }
            }) { Text("Добавить") }
        }
    }
}

@Composable
private fun GitHubTokenCard(token: String, onTokenChange: (String) -> Unit) {
    var localToken by remember(token) { mutableStateOf(token) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("GitHub Personal Access Token", style = MaterialTheme.typography.titleSmall)
            Text(
                "Нужен инструменту github_action для приватных репозиториев, создания issue и коммитов. " +
                    "Для чтения публичных репозиториев можно оставить пустым.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localToken,
                onValueChange = {
                    localToken = it
                    onTokenChange(it)
                },
                label = { Text("Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProviderRow(
    provider: AiProviderType,
    selected: Boolean,
    apiKey: String,
    onSelect: () -> Unit,
    onKeyChange: (String) -> Unit,
    models: List<ModelInfo> = emptyList(),
    onImportModel: (Uri) -> Unit = {},
    onSetActiveModel: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
    onRenameModel: (String, String) -> Unit = { _, _ -> }
) {
    var localKey by remember(provider) { mutableStateOf(apiKey) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(provider.displayName, style = MaterialTheme.typography.titleSmall)
                RadioButton(selected = selected, onClick = onSelect)
            }
            if (provider != AiProviderType.OFFLINE && provider != AiProviderType.OLLAMA) {
                OutlinedTextField(
                    value = localKey,
                    onValueChange = {
                        localKey = it
                        onKeyChange(it)
                    },
                    label = { Text("API-ключ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (provider == AiProviderType.OLLAMA) {

    Text(
        "Настраивается ниже, в разделе 🦙 Ollama Manager",
        style = MaterialTheme.typography.bodySmall
    )

} else {
                LocalModelsSection(
                    models = models,
                    onImportModel = onImportModel,
                    onSetActiveModel = onSetActiveModel,
                    onDeleteModel = onDeleteModel,
                    onRenameModel = onRenameModel
                )
            }
        }
    }
}

@Composable
private fun LocalModelsSection(
    models: List<ModelInfo>,
    onImportModel: (Uri) -> Unit,
    onSetActiveModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onRenameModel: (String, String) -> Unit
) {
    val pickModelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportModel(it) }
    }

    Column {
        Text(
            "Локальная модель (GGUF)",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                pickModelLauncher.launch(arrayOf("application/octet-stream", "*/*"))
            }
        ) {
            Text("➕ Добавить модель")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (models.isEmpty()) {
            Text(
                "Модели ещё не добавлены. Нажмите «Добавить модель», чтобы выбрать .gguf файл.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                models.forEach { model ->
                    ModelRow(
                        model = model,
                        onSetActive = { onSetActiveModel(model.path) },
                        onDelete = { onDeleteModel(model.path) },
                        onRename = { newName -> onRenameModel(model.path, newName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            val active = models.firstOrNull { it.selected }
            Text(
                "Добавлено моделей: ${models.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Активная: ${active?.name ?: "не выбрана"}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var isRenaming by remember(model.path) { mutableStateOf(false) }
    var renameText by remember(model.path) { mutableStateOf(model.name) }
    var menuExpanded by remember(model.path) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isRenaming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (renameText.isNotBlank()) {
                            onRename(renameText)
                        }
                        isRenaming = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить имя")
                    }
                    IconButton(onClick = {
                        renameText = model.name
                        isRenaming = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = if (model.selected) "Активная модель" else null,
                            tint = if (model.selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(model.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (model.selected) "⭐ Активная" else formatModelSize(model.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Действия с моделью")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (model.selected) "⭐ Активная" else "⭐ Сделать активной") },
                                enabled = !model.selected,
                                onClick = {
                                    menuExpanded = false
                                    onSetActive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("✏️ Переименовать") },
                                onClick = {
                                    menuExpanded = false
                                    isRenaming = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🗑 Удалить") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatModelSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        "%.2f ГБ".format(gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        "%.0f МБ".format(mb)
    }
}
