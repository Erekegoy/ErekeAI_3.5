package com.erekeai.features.knowledge.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.features.knowledge.viewmodel.KnowledgeBaseViewModel

/**
 * Экран "База знаний": выбор PDF/DOCX/XLSX/TXT/MD через системный File Picker,
 * реальная индексация в векторную БД (см. ImportDocumentTool) и список уже
 * проиндексированных источников.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    onBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importFromUri(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("База знаний") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePicker.launch("*/*") },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Добавить документ") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Загрузи PDF, DOCX, XLSX, TXT или MD — документ разобьётся на " +
                    "фрагменты, посчитаются эмбеддинги и агент сможет искать по " +
                    "нему через knowledge_base_search в чате.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Индексация документа...", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
            }

            state.lastMessage?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Проиндексированные документы", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (state.indexedSources.isEmpty()) {
                Text("Пока пусто", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn {
                    items(state.indexedSources) { source ->
                        Text("• $source", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
