package com.erekeai.data.tools

import android.content.Context
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Папка "документов агента" — файлы, которые пользователь явно поместил для работы с ErekeAI
 * (например, через будущий экран "Файлы"). Инструменты ниже работают только в этой песочнице,
 * поэтому не требуют опасных runtime-разрешений (READ_EXTERNAL_STORAGE и т.п.).
 */
private fun agentDocumentsDir(context: Context): File =
    File(context.filesDir, "agent_documents").apply { mkdirs() }

/**
 * Возвращает список файлов, доступных агенту, чтобы модель понимала, что можно прочитать.
 */
@Singleton
class ListFilesTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "list_files",
        description = "Возвращает список файлов, доступных агенту для чтения",
        parameters = emptyList()
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val files = agentDocumentsDir(context).listFiles()?.filter { it.isFile } ?: emptyList()
        if (files.isEmpty()) {
            ToolResult(true, "Папка документов пуста. Пользователь ещё не добавил файлы.")
        } else {
            ToolResult(true, files.joinToString("\n") { "- ${it.name} (${it.length()} байт)" })
        }
    }
}

/**
 * Читает текстовое содержимое файла по имени из папки документов агента.
 */
@Singleton
class ReadFileTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "read_file",
        description = "Читает текстовое содержимое файла по его имени из списка list_files",
        parameters = listOf(ToolParameter("filename", "точное имя файла, включая расширение"))
    )

    private val maxChars = 8000 // защита от переполнения контекста модели

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val filename = args["filename"]?.trim()
        if (filename.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'filename'")
        }

        val dir = agentDocumentsDir(context)
        val target = File(dir, filename)

        // Защита от выхода за пределы песочницы (path traversal)
        if (!target.canonicalPath.startsWith(dir.canonicalPath)) {
            return@withContext ToolResult(false, "Недопустимый путь к файлу")
        }

        if (!target.exists() || !target.isFile) {
            return@withContext ToolResult(false, "Файл '$filename' не найден. Используйте list_files, чтобы увидеть доступные файлы.")
        }

        try {
            val text = target.readText()
            val truncated = if (text.length > maxChars) text.take(maxChars) + "\n…(обрезано)" else text
            ToolResult(true, truncated)
        } catch (e: Exception) {
            ToolResult(false, "Не удалось прочитать файл: ${e.message}")
        }
    }
}
