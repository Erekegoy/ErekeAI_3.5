package com.erekeai.data.tools

import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Инструмент веб-поиска для агента. Использует HTML-версию DuckDuckGo (html.duckduckgo.com),
 * которая не требует API-ключа и подходит для сценариев без готовой поисковой подписки.
 * Возвращает топ-5 заголовков + сниппетов в виде текста для модели.
 */
@Singleton
class WebSearchTool @Inject constructor(
    private val client: OkHttpClient
) : Tool {

    override val definition = ToolDefinition(
        name = "web_search",
        description = "Ищет актуальную информацию в интернете по текстовому запросу",
        parameters = listOf(ToolParameter("query", "поисковый запрос на любом языке"))
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val query = args["query"]?.trim()
        if (query.isNullOrBlank()) {
            return@withContext ToolResult(success = false, content = "Не указан параметр 'query'")
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://html.duckduckgo.com/html/?q=$encoded")
                .header("User-Agent", "Mozilla/5.0 (Android) ErekeAI-Agent")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ToolResult(false, "Поиск не удался: HTTP ${response.code}")
                }
                val html = response.body?.string().orEmpty()
                val doc = Jsoup.parse(html)
                val results = doc.select("div.result").take(5).mapNotNull { el ->
                    val title = el.selectFirst("a.result__a")?.text()?.trim()
                    val snippet = el.selectFirst("a.result__snippet, div.result__snippet")?.text()?.trim()
                    if (title.isNullOrBlank()) null else "• $title — ${snippet.orEmpty()}"
                }

                if (results.isEmpty()) {
                    ToolResult(true, "По запросу «$query» ничего не найдено.")
                } else {
                    ToolResult(true, results.joinToString("\n"))
                }
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка веб-поиска: ${e.message}")
        }
    }
}
