package com.erekeai.data.tools.dev

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🐙 "Работа с Git и GitHub". На Android-устройстве нет git-бинаря и терминала, поэтому вместо
 * обёртки над командой git этот инструмент напрямую работает с GitHub REST API v3: смотрит
 * содержимое репозитория, читает файлы, список issues/PR/коммитов и может создать issue.
 * Токен личного доступа (Personal Access Token) хранится в [SecureKeyStore] под ключом "github"
 * (задаётся на экране "Настройки"). Без токена доступны только операции чтения публичных репозиториев
 * (с более жёстким лимитом запросов GitHub API).
 */
@Singleton
class GitHubTool @Inject constructor(
    private val client: OkHttpClient,
    private val secureKeyStore: SecureKeyStore
) : Tool {

    override val definition = ToolDefinition(
        name = "github_action",
        description = "Работает с GitHub через REST API: просмотр репозитория, файлов, issues, PR, коммитов, Actions, " +
            "создание issue/PR. action: list_repo | get_file | list_issues | create_issue | list_pulls | create_pull_request | " +
            "merge_pull_request | list_commits | list_workflow_runs",
        parameters = listOf(
            ToolParameter("action", "list_repo | get_file | list_issues | create_issue | list_pulls | create_pull_request | merge_pull_request | list_commits | list_workflow_runs"),
            ToolParameter("owner", "владелец репозитория, например octocat"),
            ToolParameter("repo", "имя репозитория"),
            ToolParameter("path", "путь к файлу/папке для list_repo и get_file", required = false),
            ToolParameter("title", "заголовок issue/PR для create_issue/create_pull_request", required = false),
            ToolParameter("body", "текст issue/PR для create_issue/create_pull_request", required = false),
            ToolParameter("state", "фильтр состояния (open/closed/all) для list_issues/list_pulls", required = false),
            ToolParameter("head", "исходная ветка (для create_pull_request), например feature-x", required = false),
            ToolParameter("base", "целевая ветка (для create_pull_request), по умолчанию main", required = false),
            ToolParameter("pull_number", "номер PR (для merge_pull_request)", required = false)
        )
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val action = args["action"]?.trim()?.lowercase()
        val owner = args["owner"]?.trim()
        val repo = args["repo"]?.trim()

        if (action.isNullOrBlank()) return@withContext ToolResult(false, "Не указан параметр 'action'")
        if (owner.isNullOrBlank() || repo.isNullOrBlank()) {
            return@withContext ToolResult(false, "Нужно указать 'owner' и 'repo'")
        }

        try {
            when (action) {
                "list_repo" -> listRepo(owner, repo, args["path"].orEmpty())
                "get_file" -> getFile(owner, repo, args["path"]?.trim())
                "list_issues" -> listIssues(owner, repo, args["state"]?.trim() ?: "open")
                "list_pulls" -> listPulls(owner, repo, args["state"]?.trim() ?: "open")
                "list_commits" -> listCommits(owner, repo)
                "create_issue" -> createIssue(owner, repo, args["title"]?.trim(), args["body"].orEmpty())
                "create_pull_request" -> createPullRequest(owner, repo, args)
                "merge_pull_request" -> mergePullRequest(owner, repo, args["pull_number"]?.trim())
                "list_workflow_runs" -> listWorkflowRuns(owner, repo)
                else -> ToolResult(false, "Неизвестное действие '$action'")
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка обращения к GitHub API: ${e.message}")
        }
    }

    /** ✅ "PR": создаёт Pull Request head → base. */
    private fun createPullRequest(owner: String, repo: String, args: Map<String, String>): ToolResult {
        val title = args["title"]?.trim() ?: return ToolResult(false, "Не указан 'title'")
        val head = args["head"]?.trim() ?: return ToolResult(false, "Не указан 'head' (исходная ветка)")
        val base = args["base"]?.trim() ?: "main"
        val payload = buildString {
            append("{")
            append("\"title\":${jsonQuote(title)},")
            append("\"head\":${jsonQuote(head)},")
            append("\"base\":${jsonQuote(base)}")
            args["body"]?.let { append(",\"body\":${jsonQuote(it)}") }
            append("}")
        }
        val url = "https://api.github.com/repos/$owner/$repo/pulls"
        val request = requestBuilder(url).post(payload.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)
            val obj = json.parseToJsonElement(text).jsonObject
            return ToolResult(true, "Pull Request #${obj["number"]?.jsonPrimitive?.content} создан: ${obj["html_url"]?.jsonPrimitive?.content}")
        }
    }

    /** ✅ "PR": сливает Pull Request. */
    private fun mergePullRequest(owner: String, repo: String, pullNumber: String?): ToolResult {
        if (pullNumber.isNullOrBlank()) return ToolResult(false, "Не указан 'pull_number'")
        val url = "https://api.github.com/repos/$owner/$repo/pulls/$pullNumber/merge"
        val request = requestBuilder(url).put("{}".toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)
            return ToolResult(true, "Pull Request #$pullNumber слит")
        }
    }

    /** ✅ "Actions": последние запуски workflow (дополняет build_apk_agent для общего обзора CI). */
    private fun listWorkflowRuns(owner: String, repo: String): ToolResult {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs?per_page=10"
        val request = requestBuilder(url).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)
            val runs = json.parseToJsonElement(text).jsonObject["workflow_runs"]?.jsonArray ?: return ToolResult(true, "Запусков нет")
            val lines = runs.mapNotNull { it as? JsonObject }.map { o ->
                "id=${o["id"]?.jsonPrimitive?.content} ${o["name"]?.jsonPrimitive?.content} status=${o["status"]?.jsonPrimitive?.content} conclusion=${o["conclusion"]?.jsonPrimitive?.content}"
            }
            return ToolResult(true, lines.joinToString("\n").ifBlank { "Запусков нет" })
        }
    }

    private fun token(): String? = secureKeyStore.getKey("github")?.takeIf { it.isNotBlank() }

    private fun requestBuilder(url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ErekeAI-DevAgent")
        token()?.let { builder.header("Authorization", "Bearer $it") }
        return builder
    }

    private fun listRepo(owner: String, repo: String, path: String): ToolResult {
        val cleanPath = path.trim('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$cleanPath"
        val request = requestBuilder(url).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)

            val element = json.parseToJsonElement(text)
            val entries = if (element is JsonArray) element else JsonArray(listOf(element))
            val lines = entries.mapNotNull { it as? JsonObject }.map { obj ->
                val name = obj["name"]?.jsonPrimitive?.content ?: "?"
                val type = obj["type"]?.jsonPrimitive?.content ?: "?"
                val size = obj["size"]?.jsonPrimitive?.content ?: "0"
                "[$type] $name ($size байт)"
            }
            return ToolResult(true, if (lines.isEmpty()) "Папка пуста." else lines.joinToString("\n"))
        }
    }

    private fun getFile(owner: String, repo: String, path: String?): ToolResult {
        if (path.isNullOrBlank()) return ToolResult(false, "Не указан параметр 'path'")
        val url = "https://api.github.com/repos/$owner/$repo/contents/${path.trim('/')}"
        val request = requestBuilder(url).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)

            val obj = json.parseToJsonElement(text).jsonObject
            val encoding = obj["encoding"]?.jsonPrimitive?.content
            val rawContent = obj["content"]?.jsonPrimitive?.content.orEmpty()
            val decoded = if (encoding == "base64") {
                String(Base64.getMimeDecoder().decode(rawContent.replace("\n", "")))
            } else {
                rawContent
            }
            val maxChars = 8000
            val truncated = if (decoded.length > maxChars) decoded.take(maxChars) + "\n…(обрезано)" else decoded
            return ToolResult(true, truncated)
        }
    }

    private fun listIssues(owner: String, repo: String, state: String): ToolResult {
        val url = "https://api.github.com/repos/$owner/$repo/issues?state=$state&per_page=15"
        val request = requestBuilder(url).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)

            val items = json.parseToJsonElement(text).jsonArray
            val lines = items.mapNotNull { it as? JsonObject }
                .filter { !it.containsKey("pull_request") } // issues endpoint также возвращает PR
                .map { obj ->
                    val number = obj["number"]?.jsonPrimitive?.content ?: "?"
                    val title = obj["title"]?.jsonPrimitive?.content ?: "?"
                    "#$number — $title"
                }
            return ToolResult(true, if (lines.isEmpty()) "Issues не найдены (state=$state)." else lines.joinToString("\n"))
        }
    }

    private fun listPulls(owner: String, repo: String, state: String): ToolResult {
        val url = "https://api.github.com/repos/$owner/$repo/pulls?state=$state&per_page=15"
        val request = requestBuilder(url).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)

            val items = json.parseToJsonElement(text).jsonArray
            val lines = items.mapNotNull { it as? JsonObject }.map { obj ->
                val number = obj["number"]?.jsonPrimitive?.content ?: "?"
                val title = obj["title"]?.jsonPrimitive?.content ?: "?"
                val branch = obj["head"]?.jsonObject?.get("ref")?.jsonPrimitive?.content ?: "?"
                "#$number — $title (ветка: $branch)"
            }
            return ToolResult(true, if (lines.isEmpty()) "Pull request'ы не найдены (state=$state)." else lines.joinToString("\n"))
        }
    }

    private fun listCommits(owner: String, repo: String): ToolResult {
        val url = "https://api.github.com/repos/$owner/$repo/commits?per_page=10"
        val request = requestBuilder(url).build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)

            val items = json.parseToJsonElement(text).jsonArray
            val lines = items.mapNotNull { it as? JsonObject }.map { obj ->
                val sha = obj["sha"]?.jsonPrimitive?.content?.take(7) ?: "?"
                val message = obj["commit"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    ?.lineSequence()?.firstOrNull() ?: "?"
                "$sha — $message"
            }
            return ToolResult(true, lines.joinToString("\n"))
        }
    }

    private fun createIssue(owner: String, repo: String, title: String?, body: String): ToolResult {
        if (title.isNullOrBlank()) return ToolResult(false, "Не указан параметр 'title'")
        if (token() == null) {
            return ToolResult(
                false,
                "Для создания issue нужен GitHub-токен. Добавьте личный токен доступа на экране Настройки."
            )
        }

        val payload = buildString {
            append("{")
            append("\"title\":${jsonQuote(title)},")
            append("\"body\":${jsonQuote(body)}")
            append("}")
        }
        val url = "https://api.github.com/repos/$owner/$repo/issues"
        val request = requestBuilder(url)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return errorResult(response.code, text)
            val obj = json.parseToJsonElement(text).jsonObject
            val number = obj["number"]?.jsonPrimitive?.content
            val htmlUrl = obj["html_url"]?.jsonPrimitive?.content
            return ToolResult(true, "Issue #$number создан: $htmlUrl")
        }
    }

    private fun jsonQuote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun errorResult(code: Int, body: String): ToolResult {
        val hint = when (code) {
            401 -> "Неверный или просроченный GitHub-токен."
            403 -> "Превышен лимит запросов GitHub API или недостаточно прав. Добавьте токен в Настройках."
            404 -> "Репозиторий, файл или путь не найдены."
            else -> "HTTP $code"
        }
        return ToolResult(false, "GitHub API: $hint\n${body.take(300)}")
    }
}
