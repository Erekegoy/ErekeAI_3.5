package com.erekeai.features.projectexplorer.tree

import com.erekeai.features.projectexplorer.model.ProjectNode
import java.io.File

object ProjectTreeBuilder {

    fun build(root: File): ProjectNode {
        return createNode(root, 0)
    }

    private fun createNode(file: File, level: Int): ProjectNode {

        val node = ProjectNode(
            file = file,
            level = level
        )

        if (file.isDirectory) {
            file.listFiles()
                ?.sortedBy { it.name.lowercase() }
                ?.forEach { child ->
                    node.children.add(
                        createNode(child, level + 1)
                    )
                }
        }

        return node
    }
}
