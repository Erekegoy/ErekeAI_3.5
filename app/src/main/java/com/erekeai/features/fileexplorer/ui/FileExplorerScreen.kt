package com.erekeai.features.fileexplorer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    onBack: () -> Unit
) {
    var currentDir by remember {
        mutableStateOf(File("/storage/emulated/0"))
    }

    val files = remember(currentDir) {
        currentDir.listFiles()?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(currentDir.absolutePath)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (currentDir.parentFile != null) {
                item {
                    ListItem(
                        headlineContent = {
                            Text("⬅ ..")
                        },
                        modifier = Modifier.clickable {
                            currentDir.parentFile?.let {
                                currentDir = it
                            }
                        }
                    )
                }
            }

            items(files) { file ->

                ListItem(
                    leadingContent = {
                        Icon(Icons.Default.Folder, null)
                    },
                    headlineContent = {
                        Text(file.name)
                    },
                    supportingContent = {
                        Text(
                            if (file.isDirectory)
                                "Папка"
                            else
                                "${file.length()} байт"
                        )
                    },
                    modifier = Modifier.clickable {
                        if (file.isDirectory) {
                            currentDir = file
                        }
                    }
                )

                HorizontalDivider()
            }
        }
    }
}
