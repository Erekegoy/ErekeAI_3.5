package com.erekeai.data.tools.dev

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
 * 🧪 "Запуск проверок" — на мобильном устройстве нет доступа к gradle/lint/CI, поэтому
 * этот инструмент выполняет набор быстрых офлайн-эвристик прямо над текстом файла:
 * длинные строки, несбалансированные скобки, TODO/FIXME, пустые catch-блоки, смешение
 * табов и пробелов, висящие пробелы в конце строк. Это не замена полноценному линтеру,
 * а быстрый предварительный отчёт, который агент может предложить пользователю.
 */
@Singleton
class StaticCheckTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "run_static_checks",
        description = "Выполняет быстрые офлайн-проверки файла: длинные строки, несбалансированные скобки, " +
            "TODO/FIXME, пустые catch-блоки, висящие пробелы",
        parameters = listOf(ToolParameter("filename", "имя файла в песочнице агента, включая подпапки"))
    )

    private val maxLineLength = 120

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val filename = args["filename"]?.trim()
        if (filename.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'filename'")
        }

        val root = devSandboxDir(context)
        val target = File(root, filename)
        if (!isWithinSandbox(root, target)) {
            return@withContext ToolResult(false, "Недопустимый путь к файлу")
        }
        if (!target.exists() || !target.isFile) {
            return@withContext ToolResult(false, "Файл '$filename' не найден")
        }

        val lines = try {
            target.readLines()
        } catch (e: Exception) {
            return@withContext ToolResult(false, "Не удалось прочитать файл: ${e.message}")
        }

        val issues = mutableListOf<String>()

        val longLines = lines.withIndex().filter { it.value.length > maxLineLength }
        if (longLines.isNotEmpty()) {
            issues.add("Строки длиннее $maxLineLength символов (${longLines.size}): " +
                longLines.take(10).joinToString(", ") { "стр. ${it.index + 1}" })
        }

        val todoLines = lines.withIndex().filter { it.value.contains("TODO") || it.value.contains("FIXME") }
        if (todoLines.isNotEmpty()) {
            issues.add("TODO/FIXME (${todoLines.size}): " +
                todoLines.take(10).joinToString(", ") { "стр. ${it.index + 1}" })
        }

        val trailingWhitespace = lines.withIndex().filter { it.value != it.value.trimEnd() && it.value.isNotEmpty() }
        if (trailingWhitespace.isNotEmpty()) {
            issues.add("Висящие пробелы в конце строки (${trailingWhitespace.size} строк)")
        }

        val mixedIndent = lines.any { it.startsWith(" \t") || it.startsWith("\t ") }
        if (mixedIndent) {
            issues.add("Обнаружено смешение табов и пробелов в отступах")
        }

        val fullText = lines.joinToString("\n")
        val balance = checkBracketBalance(fullText)
        if (balance != null) {
            issues.add("Похоже на несбалансированные скобки: $balance (проверка эвристическая, строки в комментариях/строках не учитываются)")
        }

        val emptyCatchRegex = Regex("""catch\s*\([^)]*\)\s*\{\s*\}""")
        val emptyCatchCount = emptyCatchRegex.findAll(fullText).count()
        if (emptyCatchCount > 0) {
            issues.add("Пустые catch-блоки (проглатывание ошибок): $emptyCatchCount")
        }

        val report = if (issues.isEmpty()) {
            "Проверка '$filename': явных проблем не найдено (${lines.size} строк)."
        } else {
            "Проверка '$filename' (${lines.size} строк) — найдено ${issues.size} категорий замечаний:\n" +
                issues.joinToString("\n") { "  • $it" }
        }

        ToolResult(true, report)
    }

    /** Простой счётчик скобок () [] {} без учёта строковых литералов и комментариев — эвристика. */
    private fun checkBracketBalance(text: String): String? {
        val pairs = mapOf('(' to ')', '[' to ']', '{' to '}')
        val closing = pairs.values.toSet()
        val stack = ArrayDeque<Char>()
        for (ch in text) {
            when {
                pairs.containsKey(ch) -> stack.addLast(ch)
                ch in closing -> {
                    val expectedOpen = pairs.entries.firstOrNull { it.value == ch }?.key
                    if (stack.isEmpty() || stack.removeLast() != expectedOpen) {
                        return "неожиданная закрывающая скобка '$ch'"
                    }
                }
            }
        }
        return if (stack.isNotEmpty()) "не закрыты скобки: ${stack.joinToString("") { it.toString() }}" else null
    }
}
