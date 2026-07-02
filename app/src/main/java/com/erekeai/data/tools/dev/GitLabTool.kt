package com.erekeai.data.tools.dev

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "Интеграция с GitLab (Issues, Merge Requests, Pipelines)" — через GitLab REST API v4.
 * Токен (Personal/Project Access Token, scope api) хранится в SecureKeyStore под ключом "gitlab",
 * задаётся в Настройках. Поддерживает как gitlab.com, так и self-hosted (параметр 'host').
 *
 * action: list_issues | create_issue | list_merge_requests | create_merge_request | list_pipelines | trigger_pipeline
 */
@Singleton
class GitLabTool @Inject constructor(
    private val client: OkHttpClient,
    private val secureKeyStore: SecureKeyStore
) : Tool {

    override val definition = ToolDefinition(
        name = "gitlab_action",
        description = "Работает с GitLab через REST API v4: list_issues, create_issue, list_merge_requests, create_merge_request, list_pipelines, trigger_pipeline",
        parameters = listOf(
            ToolParameter("action", "list_issues | create_issue | list_merge_requests | create_merge_request | list_pipelines | trigger_pipeline"),
            ToolParameter("project_id", "числовой id или url-encoded путь проекта (owner%2Frepo)"),
            ToolParameter("host", "хост GitLab, по умолчанию gitlab.com (для self-hosted)", required = false),
            ToolParameter("title", "заголовок issue/MR (для create_issue/create_merge_request)", required = false),
            ToolParameter("description", "описание issue/MR", required = false),
            ToolParameter("source_branch", "исходная ветка (для create_merge_request)", required = false),
            ToolParameter("target_branch", "целевая ветка (для create_merge_request), по умолчанию main", required = false),
            ToolParameter("ref", "ветка для trigger_pipeline, по умолчанию main", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val token = secureKeyStore.getKey("gitlab")
            ?: return@withContext ToolResult(false, "GitLab не настроен: добавьте Personal Access Token (scope api) в Настройках → GitLab")
        val host = args["host"]?.trim()?.ifBlank { null } ?: "gitlab.com"
        val projectId = args["project_id"]?.trim() ?: return@withContext ToolResult(false, "Не указан 'project_id'")
        val base = "https://$host/api/v4/projects/$projectId"

        try {
            when (args["action"]?.trim()) {
                "list_issues" -> listIssues(base, token)
                "create_issue" -> createIssue(base, args, token)
                "list_merge_requests" -> listMergeRequests(base, token)
                "create_merge_request" -> createMergeRequest(base, args, token)
                "list_pipelines" -> listPipelines(base, token)
                "trigger_pipeline" -> triggerPipeline(base, args, token)
                else -> ToolResult(false, "Не указан или неизвестен 'action'")
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка GitLab API: ${e.message}")
        }
    }

    private fun authRequest(url: String, token: String) = Request.Builder().url(url).addHeader("PRIVATE-TOKEN", token)

    private fun listIssues(base: String, token: String): ToolResult {
        val request = authRequest("$base/issues?state=opened&per_page=15", token).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}")
            val arr = JSONArray(response.body?.string().orEmpty())
            if (arr.length() == 0) return ToolResult(true, "Открытых issue нет")
            return ToolResult(true, (0 until arr.length()).joinToString("\n") { i -> val o = arr.getJSONObject(i); "#${o.optInt("iid")} — ${o.optString("title")}" })
        }
    }

    private fun createIssue(base: String, args: Map<String, String>, token: String): ToolResult {
        val title = args["title"] ?: return ToolResult(false, "Не указан 'title'")
        val body = JSONObject().apply { put("title", title); args["description"]?.let { put("description", it) } }.toString()
        val request = authRequest("$base/issues", token).post(body.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}: ${response.body?.string()?.take(300)}")
            val obj = JSONObject(response.body?.string().orEmpty())
            return ToolResult(true, "Issue #${obj.optInt("iid")} создан: ${obj.optString("web_url")}")
        }
    }

    private fun listMergeRequests(base: String, token: String): ToolResult {
        val request = authRequest("$base/merge_requests?state=opened&per_page=15", token).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}")
            val arr = JSONArray(response.body?.string().orEmpty())
            if (arr.length() == 0) return ToolResult(true, "Открытых merge request'ов нет")
            return ToolResult(true, (0 until arr.length()).joinToString("\n") { i ->
                val o = arr.getJSONObject(i); "!${o.optInt("iid")} — ${o.optString("title")} (${o.optString("source_branch")} → ${o.optString("target_branch")})"
            })
        }
    }

    private fun createMergeRequest(base: String, args: Map<String, String>, token: String): ToolResult {
        val title = args["title"] ?: return ToolResult(false, "Не указан 'title'")
        val sourceBranch = args["source_branch"] ?: return ToolResult(false, "Не указан 'source_branch'")
        val targetBranch = args["target_branch"] ?: "main"
        val body = JSONObject().apply {
            put("title", title); put("source_branch", sourceBranch); put("target_branch", targetBranch)
            args["description"]?.let { put("description", it) }
        }.toString()
        val request = authRequest("$base/merge_requests", token).post(body.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}: ${response.body?.string()?.take(300)}")
            val obj = JSONObject(response.body?.string().orEmpty())
            return ToolResult(true, "Merge request !${obj.optInt("iid")} создан: ${obj.optString("web_url")}")
        }
    }

    private fun listPipelines(base: String, token: String): ToolResult {
        val request = authRequest("$base/pipelines?per_page=10", token).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}")
            val arr = JSONArray(response.body?.string().orEmpty())
            return ToolResult(true, (0 until arr.length()).joinToString("\n") { i -> val o = arr.getJSONObject(i); "id=${o.optLong("id")} status=${o.optString("status")}" })
        }
    }

    private fun triggerPipeline(base: String, args: Map<String, String>, token: String): ToolResult {
        val ref = args["ref"] ?: "main"
        val request = authRequest("$base/pipeline?ref=$ref", token).post("".toRequestBody(null)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "HTTP ${response.code}: ${response.body?.string()?.take(300)}")
            val obj = JSONObject(response.body?.string().orEmpty())
            return ToolResult(true, "Pipeline запущен: id=${obj.optLong("id")}, статус=${obj.optString("status")}")
        }
    }
}
