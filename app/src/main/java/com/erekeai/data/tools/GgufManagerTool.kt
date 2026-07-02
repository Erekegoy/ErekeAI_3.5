package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import com.erekeai.gguf.GgufManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GgufManagerTool @Inject constructor(
    private val ggufManager: GgufManager
) : Tool {
    override val definition = ToolDefinition(
        name = "gguf_manager",
        description = "Управляет файлами GGUF-моделей в хранилище приложения: list, download, delete " +
            "(ЧЕСТНО: инференс на устройстве пока не подключён, только хранение файлов — см. описание инструмента)",
        parameters = listOf(
            ToolParameter("action", "list | download | delete"),
            ToolParameter("url", "прямая ссылка на .gguf файл (для download)", required = false),
            ToolParameter("filename", "имя файла (для download/delete)", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        return when (args["action"]?.trim()) {
            "list" -> {
                val models = ggufManager.listModels()
                if (models.isEmpty()) ToolResult(true, "Файлов GGUF пока нет.")
                else ToolResult(true, models.joinToString("\n") { "- ${it.name} (${it.sizeBytes / (1024 * 1024)} МБ)" })
            }
            "download" -> {
                val url = args["url"] ?: return ToolResult(false, "Не указан 'url'")
                val filename = args["filename"] ?: return ToolResult(false, "Не указан 'filename'")
                val result = ggufManager.download(url, filename)
                result.fold(
                    onSuccess = { ToolResult(true, "Скачано: ${it.name} (${it.sizeBytes / (1024 * 1024)} МБ). Инференс на устройстве пока не подключён (см. описание инструмента).") },
                    onFailure = { ToolResult(false, "Ошибка загрузки: ${it.message}") }
                )
            }
            "delete" -> {
                val filename = args["filename"] ?: return ToolResult(false, "Не указан 'filename'")
                if (ggufManager.delete(filename)) ToolResult(true, "Удалено: $filename") else ToolResult(false, "Файл не найден")
            }
            else -> ToolResult(false, "Не указан или неизвестен 'action'. Доступны: list, download, delete")
        }
    }
}
