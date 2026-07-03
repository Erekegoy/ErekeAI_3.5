package com.erekeai.features.codeeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    fileName: String,
    content: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val viewModel: CodeEditorViewModel = hiltViewModel()
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
                        onSave(text)
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
        value = text,
        onValueChange = {
            text = it
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        singleLine = false,
        maxLines = Int.MAX_VALUE
    )

}

}
