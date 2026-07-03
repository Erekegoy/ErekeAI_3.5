package com.erekeai.features.projectexplorer.viewmodel

import androidx.lifecycle.ViewModel
import com.erekeai.features.projectexplorer.model.ProjectNode
import com.erekeai.features.projectexplorer.tree.ProjectTreeBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProjectExplorerViewModel @Inject constructor() : ViewModel() {

    private val _root = MutableStateFlow<ProjectNode?>(null)
    val root = _root.asStateFlow()

    fun open(rootDir: File) {
        _root.value = ProjectTreeBuilder.build(rootDir)
    }

    fun toggle(node: ProjectNode) {
        node.expanded = !node.expanded
        _root.value = _root.value
    }
}
