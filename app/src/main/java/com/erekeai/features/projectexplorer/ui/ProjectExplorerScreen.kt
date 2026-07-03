package com.erekeai.features.projectexplorer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erekeai.features.projectexplorer.model.ProjectNode
import com.erekeai.features.projectexplorer.viewmodel.ProjectExplorerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectExplorerScreen(
    root: File,
    onBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    viewModel: ProjectExplorerViewModel = hiltViewModel()
) {

    val nodes by viewModel.nodes.collectAsState()

    LaunchedEffect(root) {
        viewModel.open(root)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Explorer") },
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

            items(nodes) { node ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (node.isDirectory)
                                viewModel.toggle(node)
                            else
                                onOpenFile(node.file)
                        }
                        .padding(start = (node.level * 16).dp)
                ) {

                    Icon(
                        if (node.isDirectory)
                            Icons.Default.Folder
                        else
                            Icons.Default.InsertDriveFile,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(node.name)
                }
            }
        }
    }
}
