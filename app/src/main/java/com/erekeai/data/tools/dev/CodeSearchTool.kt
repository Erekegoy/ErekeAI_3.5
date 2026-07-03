package com.erekeai.data.tools.dev

import android.content.Context
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💻 "Анализ структуры кода" — поиск по содержимому файлов в песочнице агента (аналог grep).
 * Позволяет модели быстро находить объявления функций/классов, использования символа,
 * TODO-метки и т.д., не читая каждый файл целиком по отдельности.
 */
@Singleton
class CodeSearchTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "code_search",
        description = "Ищет текстовую подстроку по всем файлам проекта в песочнице агента (аналог grep)",
        parameters = listOf(
            ToolParameter("query", "текст для поиска (без regex, обычная подстрока)"),
            ToolParameter("extension", "ограничить поиск файлами с данным расширением, например kt", required = false)
        )
    )

    private val maxMatches = 40
    private val maxFilesToScan = 500

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.trim()
        val extensionFilter = args["extension"]?.trim()?.removePrefix(".")?.lowercase()

        if (query.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'query'")
        }

        val root = devSandboxDir(context)
        val files = root.walkTopDown()
            .filter { it.isFile }
            .filter { extensionFilter == null || it.extension.lowercase() == extensionFilter }
            .take(maxFilesToScan)
            .toList()

        if (files.isEmpty()) {
            return@withContext ToolResult(true, "В песочнице нет файлов для поиска (проверьте параметр 'extension').")
        }

        val matches = mutableListOf<String>()
        outer@ for (file in files) {
            val lines = try {
                file.readLines()
            } catch (_: Exception) {
                continue@outer // бинарный/нечитаемый файл
            }
            for ((index, line) in lines.withIndex()) {
    if (matches.size >= maxMatches) break@outer

    if (line.contains(query, ignoreCase = true)) {
        val relPath = file.relativeTo(root).path
        matches.add("${relPath}:${index + 1}: ${line.trim().take(160)}")
    }
}
        }

        if (matches.isEmpty()) {
            ToolResult(true, "По запросу «$query» совпадений не найдено.")
        } else {
            val suffix = if (matches.size >= maxMatches) "\n…(показаны первые $maxMatches совпадений)" else ""
            ToolResult(true, matches.joinToString("\n") + suffix)
        }
    }
}
