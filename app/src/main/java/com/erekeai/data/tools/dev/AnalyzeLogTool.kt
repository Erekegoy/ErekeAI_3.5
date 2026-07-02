package com.erekeai.data.tools.dev

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧪 "Анализ логов" — принимает вставленный пользователем текст лога/стектрейса и
 * выделяет из него полезные для диагностики сведения: тип и сообщение исключения,
 * первую "свою" строку стектрейса, количество вложенных причин (Caused by),
 * плюс типовую подсказку по частым исключениям. Работает полностью офлайн,
 * без обращения к AI-провайдеру — это быстрый детерминированный разбор текста.
 */
@Singleton
class AnalyzeLogTool @Inject constructor() : Tool {

    override val definition = ToolDefinition(
        name = "analyze_log",
        description = "Разбирает вставленный лог/стектрейс: тип исключения, сообщение, вероятная причина",
        parameters = listOf(
            ToolParameter("log", "текст лога или стектрейса для анализа"),
            ToolParameter(
                "package_hint",
                "префикс пакета приложения, чтобы найти 'свою' строку стектрейса (по умолчанию com.erekeai)",
                required = false
            )
        )
    )

    private val maxChars = 12_000

    private val exceptionRegex = Regex("""([A-Za-z_][A-Za-z0-9_.]*(?:Exception|Error))(?::\s*(.*))?""")

    private val knownHints = mapOf(
        "NullPointerException" to "Обращение к null-ссылке. Проверьте, что объект инициализирован до использования, добавьте null-проверки или используйте безопасные вызовы (?.).",
        "IndexOutOfBoundsException" to "Обращение к элементу за пределами коллекции/массива. Проверьте границы индекса перед доступом.",
        "ClassCastException" to "Некорректное приведение типа. Проверьте реальный тип объекта перед приведением (is/as?).",
        "IOException" to "Ошибка ввода-вывода. Проверьте доступность файла/сети и корректно обрабатывайте исключение.",
        "OutOfMemoryError" to "Нехватка памяти. Проверьте утечки памяти и объём загружаемых данных (например, больших изображений).",
        "NetworkOnMainThreadException" to "Сетевой вызов выполняется в главном потоке UI. Перенесите его в корутину/фоновый поток.",
        "IllegalStateException" to "Операция вызвана в неподходящем состоянии объекта. Проверьте порядок вызовов и жизненный цикл компонента.",
        "IllegalArgumentException" to "Передан недопустимый аргумент. Проверьте валидацию входных данных перед вызовом.",
        "NumberFormatException" to "Ошибка преобразования строки в число. Проверьте формат входной строки перед парсингом.",
        "ConcurrentModificationException" to "Изменение коллекции во время итерации. Используйте копию коллекции или потокобезопасные структуры данных.",
        "SQLiteException" to "Ошибка базы данных SQLite. Проверьте схему, миграции и корректность SQL-запроса.",
        "SocketTimeoutException" to "Истекло время ожидания сетевого запроса. Проверьте таймауты клиента и доступность сервера."
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val log = args["log"]
        if (log.isNullOrBlank()) {
            return ToolResult(false, "Не указан параметр 'log'")
        }
        val packageHint = args["package_hint"]?.trim()?.ifBlank { null } ?: "com.erekeai"

        val truncated = if (log.length > maxChars) log.take(maxChars) else log
        val lines = truncated.lines()

        val exceptionMatch = exceptionRegex.find(truncated)
        val exceptionType = exceptionMatch?.groupValues?.get(1)
        val exceptionMessage = exceptionMatch?.groupValues?.get(2)?.trim()?.takeIf { it.isNotBlank() }

        val ownFrame = lines.firstOrNull { it.contains(packageHint) && it.trim().startsWith("at ") }
        val causedByCount = Regex("""Caused by:""").findAll(truncated).count()

        val hint = exceptionType?.let { type ->
            knownHints.entries.firstOrNull { type.endsWith(it.key) }?.value
        } ?: "Не удалось точно определить тип исключения по тексту — уточните формат лога или пришлите полный стектрейс."

        val report = buildString {
            appendLine("Строк в логе: ${lines.size}${if (log.length > maxChars) " (обрезано до $maxChars символов)" else ""}")
            appendLine("Тип исключения: ${exceptionType ?: "не определён"}")
            if (exceptionMessage != null) appendLine("Сообщение: $exceptionMessage")
            if (ownFrame != null) appendLine("Первая строка в коде приложения ('$packageHint'): ${ownFrame.trim()}")
            if (causedByCount > 0) appendLine("Вложенных причин (Caused by): $causedByCount")
            appendLine()
            appendLine("Вероятная причина и рекомендация: $hint")
        }.trim()

        return ToolResult(true, report)
    }
}
