package com.erekeai.features.projectexplorer.viewmodel

import androidx.lifecycle.ViewModel
import com.erekeai.features.projectexplorer.model.ProjectNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProjectExplorerViewModel @Inject constructor() : ViewModel() {

    private val _nodes = MutableStateFlow<List<ProjectNode>>(emptyList())
    val nodes = _nodes.asStateFlow()

    fun open(root: File) {
        _nodes.value = buildTree(root)
    }

    private fun buildTree(
        file: File,
        level: Int = 0
    ): List<ProjectNode> {

        val result = mutableListOf<ProjectNode>()

        file.listFiles()
            ?.sortedBy { it.name.lowercase() }
            ?.forEach { child ->

                val node = ProjectNode(
                    file = child,
                    level = level
                )

                result += node
            }

        return result
    }

    fun toggle(node: ProjectNode) {

        if (!node.isDirectory) return

        node.expanded = !node.expanded

        _nodes.value = _nodes.value.toList()
    }
}
