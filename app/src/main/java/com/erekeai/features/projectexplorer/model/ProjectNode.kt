package com.erekeai.features.projectexplorer.model

import java.io.File

data class ProjectNode(
    val file: File,
    val level: Int = 0,
    var expanded: Boolean = false,
    val children: MutableList<ProjectNode> = mutableListOf()
) {
    val isDirectory: Boolean
        get() = file.isDirectory

    val name: String
        get() = file.name.ifBlank { file.absolutePath }
}
