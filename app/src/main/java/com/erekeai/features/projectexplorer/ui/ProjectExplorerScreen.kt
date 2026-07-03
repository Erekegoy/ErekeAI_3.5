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

    val rootNode by viewModel.root.collectAsState()

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

            rootNode?.children?.let { children ->

                items(children) { node ->
                    ProjectNodeItem(
                        node = node,
                        onOpenFile = onOpenFile,
                        onToggle = viewModel::toggle
                    )
                }

            }
        }
    }
}

@Composable
private fun ProjectNodeItem(
    node: ProjectNode,
    onOpenFile: (File) -> Unit,
    onToggle: (ProjectNode) -> Unit
) {

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory)
                        onToggle(node)
                    else
                        onOpenFile(node.file)
                }
                .padding(start = (node.level * 16).dp, top = 6.dp, bottom = 6.dp)
        ) {

            Icon(
                imageVector =
                    if (node.isDirectory)
                        Icons.Default.Folder
                    else
                        Icons.Default.InsertDriveFile,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text(node.name)
        }

        if (node.expanded) {

            node.children.forEach { child ->

                ProjectNodeItem(
                    node = child,
                    onOpenFile = onOpenFile,
                    onToggle = onToggle
                )

            }

        }

    }
}
