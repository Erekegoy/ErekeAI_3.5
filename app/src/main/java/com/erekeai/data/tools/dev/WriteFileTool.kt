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
 * Создаёт или перезаписывает файл в песочнице агента ("agent_documents"). Ключевой инструмент
 * для сценариев ✍️ "Генерация нового кода" и 🔧 "Исправление ошибок и рефакторинг" — модель
 * сначала читает исходный файл (read_file), затем сохраняет новую версию через write_file.
 * Поддерживает вложенные пути (например, "src/Main.kt") — недостающие папки создаются автоматически.
 */
@Singleton
class WriteFileTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "write_file",
        description = "Создаёт новый файл или перезаписывает существующий в песочнице агента. " +
            "Используй для генерации кода, исправления ошибок и рефакторинга.",
        parameters = listOf(
            ToolParameter("filename", "имя файла, можно с подпапками, например src/Main.kt"),
            ToolParameter("content", "полное содержимое файла, которое нужно сохранить"),
            ToolParameter("append", "\"true\", чтобы дописать в конец файла вместо перезаписи", required = false)
        )
    )

    private val maxChars = 200_000 // защита от чрезмерно больших файлов на мобильном устройстве

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val filename = args["filename"]?.trim()
        val content = args["content"]
        val append = args["append"]?.trim()?.equals("true", ignoreCase = true) == true

        if (filename.isNullOrBlank()) {
            return@withContext ToolResult(false, "Не указан параметр 'filename'")
        }
        if (content == null) {
            return@withContext ToolResult(false, "Не указан параметр 'content'")
        }
        if (content.length > maxChars) {
            return@withContext ToolResult(false, "Содержимое файла превышает лимит в $maxChars символов")
        }

        val dir = devSandboxDir(context)
        val target = File(dir, filename)

        if (!isWithinSandbox(dir, target)) {
            return@withContext ToolResult(false, "Недопустимый путь к файлу")
        }

        try {
            target.parentFile?.mkdirs()
            if (append && target.exists()) {
                target.appendText(content)
            } else {
                target.writeText(content)
            }
            val action = if (append && target.exists()) "дописано в" else "сохранено в"
            ToolResult(true, "Содержимое $action файл '${target.relativeTo(dir).path}' (${content.length} симв.)")
        } catch (e: Exception) {
            ToolResult(false, "Не удалось записать файл: ${e.message}")
        }
    }
}
