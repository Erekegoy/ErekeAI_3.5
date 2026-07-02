package com.erekeai.data.tools.dev

import android.content.Context
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💻 "Анализ проекта и структуры кода". Рекурсивно обходит песочницу агента и строит краткую
 * сводку: дерево папок, количество файлов и строк по расширениям, самые большие файлы,
 * количество TODO/FIXME-меток. Не требует сети и не грузит в модель весь код целиком —
 * только агрегированную статистику, чтобы модель могла решить, какие файлы читать дальше.
 */
@Singleton
class AnalyzeProjectTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "analyze_project",
        description = "Анализирует структуру проекта в песочнице агента: файлы, папки, " +
            "статистика по расширениям, размер, TODO/FIXME-метки",
        parameters = emptyList()
    )

    private val maxFilesToScan = 500

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val root = devSandboxDir(context)
        val allFiles = root.walkTopDown()
            .filter { it.isFile }
            .take(maxFilesToScan)
            .toList()

        if (allFiles.isEmpty()) {
            return@withContext ToolResult(
                true,
                "Песочница проекта пуста. Добавьте файлы проекта, чтобы агент мог их проанализировать."
            )
        }

        val byExtension = allFiles.groupBy { it.extension.ifBlank { "(без расширения)" } }
        var totalLines = 0
        var todoCount = 0
        val largest = mutableListOf<Pair<File, Long>>()

        for (file in allFiles) {
            largest.add(file to file.length())
            if (isProbablyText(file)) {
                try {
                    val lines = file.readLines()
                    totalLines += lines.size
                    todoCount += lines.count { it.contains("TODO") || it.contains("FIXME") }
                } catch (_: Exception) {
                    // бинарные/нечитаемые файлы — пропускаем построчный подсчёт
                }
            }
        }

        val topDirs = root.listFiles()?.filter { it.isDirectory }?.map { it.name }.orEmpty()

        val report = buildString {
            appendLine("Проект: ${allFiles.size} файлов, $totalLines строк кода/текста, $todoCount TODO/FIXME-меток")
            appendLine()
            appendLine("Папки верхнего уровня: ${if (topDirs.isEmpty()) "нет (файлы лежат в корне)" else topDirs.joinToString(", ")}")
            appendLine()
            appendLine("По расширениям:")
            byExtension.entries.sortedByDescending { it.value.size }.forEach { (ext, files) ->
                appendLine("  .$ext — ${files.size} файлов")
            }
            appendLine()
            appendLine("Самые крупные файлы:")
            largest.sortedByDescending { it.second }.take(5).forEach { (file, size) ->
                appendLine("  ${file.relativeTo(root).path} — $size байт")
            }
            if (allFiles.size >= maxFilesToScan) {
                appendLine()
                appendLine("⚠️ Показаны первые $maxFilesToScan файлов, в проекте может быть больше.")
            }
        }.trim()

        ToolResult(true, report)
    }

    private fun isProbablyText(file: File): Boolean {
        val textExtensions = setOf(
            "kt", "java", "xml", "gradle", "kts", "md", "txt", "json", "yml", "yaml",
            "properties", "gitignore", "pro", "toml", "html", "css", "js", "ts", "py", "sql"
        )
        return file.extension.lowercase() in textExtensions || file.length() < 2_000_000
    }
}
