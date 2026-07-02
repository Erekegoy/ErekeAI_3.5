package com.erekeai.data.tools.dev

import android.content.Context
import java.io.File

/**
 * Та же песочница, что использует [com.erekeai.data.tools.ListFilesTool] / [com.erekeai.data.tools.ReadFileTool]
 * ("agent_documents") — пользователь помещает туда файлы проекта, с которыми должен работать
 * 🤖 AI Developer Agent (читать, анализировать, изменять). Вынесено в отдельный файл, чтобы
 * им могли пользоваться все инструменты пакета data.tools.dev, не создавая циклическую зависимость.
 */
internal fun devSandboxDir(context: Context): File =
    File(context.filesDir, "agent_documents").apply { mkdirs() }

/** Проверка, что путь [target] не выходит за пределы песочницы [root] (защита от path traversal). */
internal fun isWithinSandbox(root: File, target: File): Boolean =
    target.canonicalPath.startsWith(root.canonicalPath)
