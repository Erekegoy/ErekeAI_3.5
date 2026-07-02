package com.erekeai.data.tools

import com.erekeai.browser.BrowserAutomationController
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserAutomationTool @Inject constructor(
    private val controller: BrowserAutomationController
) : Tool {
    override val definition = ToolDefinition(
        name = "browser_automation",
        description = "Открывает страницу в настоящем браузерном движке (WebView с JS) и выполняет на ней JavaScript: " +
            "клик по элементу, заполнение формы, чтение динамического (SPA) контента. " +
            "Примеры script: 'document.querySelector(\"button.accept\").click(); \"ok\"' или 'document.title'",
        parameters = listOf(
            ToolParameter("url", "URL страницы"),
            ToolParameter("script", "JavaScript-выражение, результат которого будет возвращён (по умолчанию — весь текст страницы)", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val url = args["url"]?.trim() ?: return ToolResult(false, "Не указан 'url'")
        val script = args["script"] ?: ""
        return try {
            val result = controller.navigateAndRun(url, script)
            ToolResult(true, result)
        } catch (e: Exception) {
            ToolResult(false, "Ошибка автоматизации браузера: ${e.message}")
        }
    }
}
