package com.erekeai.features.codeeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier


import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erekeai.features.codeeditor.viewmodel.CodeEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    fileName: String,
    content: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {

val viewModel: CodeEditorViewModel = hiltViewModel()
val uiState by viewModel.uiState.collectAsState()
LaunchedEffect(fileName) {
    viewModel.open(fileName, content)
}    
    var text by remember(content) {
        mutableStateOf(content)
    }

    Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text(fileName)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null)
                }
            },
            actions = {
                IconButton(
    onClick = {
        onSave(uiState.text)
        viewModel.status("✅ Файл сохранён")
    }
) {
                    Icon(Icons.Default.Save, null)
                }
            }
        )
    },

    bottomBar = {
        BottomAppBar {

            TextButton(onClick = { }) {
                Text("🤖 Explain")
            }

            TextButton(onClick = { }) {
                Text("🔧 Fix")
            }

            TextButton(onClick = { }) {
                Text("✨ Refactor")
            }

            TextButton(onClick = { }) {
                Text("▶ Run")
            }
        }
    }

) { padding ->

    OutlinedTextField(
    value = uiState.text,
    onValueChange = {
        viewModel.update(it)
    },
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        singleLine = false,
        maxLines = Int.MAX_VALUE
    )

}

}
