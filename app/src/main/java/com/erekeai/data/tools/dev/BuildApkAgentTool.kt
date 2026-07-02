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
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ ЧЕСТНО про "Build APK Agent": скомпилировать Android-проект (Gradle + Android SDK/AAPT2/D8,
 * несколько ГБ инструментов, десятки минут) прямо НА ТЕЛЕФОНЕ нереалистично — на устройстве нет
 * ни Android SDK, ни JDK, ни гигабайт свободного места и батареи под это, и ни один существующий
 * инструмент такого на телефоне не делает.
 *
 * Реалистичный и РЕАЛЬНО работающий путь — то, как это делают профессиональные mobile-agent
 * инструменты: телефон не собирает APK сам, а ЗАПУСКАЕТ сборку в облаке (GitHub Actions CI
 * репозитория) через GitHub REST API (workflow_dispatch), а потом проверяет статус и присылает
 * ссылку на готовый артефакт. Требования: в репозитории должен быть настроен workflow сборки
 * (обычно .github/workflows/build.yml с шагами `./gradlew assembleRelease` и `actions/upload-artifact`)
 * и GitHub-токен с правом `workflow` в Настройках (тот же токен, что и для github_action).
 *
 * action: "trigger" (owner, repo, workflow_file, ref) | "check_status" (owner, repo, run_id)
 */
@Singleton
class BuildApkAgentTool @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val httpClient: OkHttpClient
) : Tool {

    override val definition = ToolDefinition(
        name = "build_apk_agent",
        description = "Запускает сборку APK через GitHub Actions CI (не на устройстве!) и проверяет статус: trigger, check_status",
        parameters = listOf(
            ToolParameter("action", "trigger | check_status"),
            ToolParameter("owner", "владелец репозитория"),
            ToolParameter("repo", "имя репозитория"),
            ToolParameter("workflow_file", "имя файла workflow, например build.yml (для trigger)", required = false),
            ToolParameter("ref", "ветка для сборки, по умолчанию main (для trigger)", required = false),
            ToolParameter("run_id", "id запуска из ответа trigger (для check_status)", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val token = secureKeyStore.getKey("github")
            ?: return@withContext ToolResult(false, "Нужен GitHub-токен (scope: workflow) в Настройках → GitHub")
        val owner = args["owner"]?.trim() ?: return@withContext ToolResult(false, "Не указан 'owner'")
        val repo = args["repo"]?.trim() ?: return@withContext ToolResult(false, "Не указан 'repo'")

        when (args["action"]?.trim()) {
            "trigger" -> trigger(owner, repo, args["workflow_file"] ?: "build.yml", args["ref"] ?: "main", token)
            "check_status" -> checkStatus(owner, repo, args["run_id"], token)
            else -> ToolResult(false, "Не указан или неизвестен 'action'. Доступны: trigger, check_status")
        }
    }

    private fun authRequest(url: String, token: String) = Request.Builder()
        .url(url)
        .addHeader("Accept", "application/vnd.github+json")
        .addHeader("Authorization", "Bearer $token")

    private fun trigger(owner: String, repo: String, workflowFile: String, ref: String, token: String): ToolResult {
        val url = "https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowFile/dispatches"
        val body = JSONObject().apply { put("ref", ref) }.toString()
        val request = authRequest(url, token).post(body.toRequestBody("application/json".toMediaType())).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return ToolResult(false, "Не удалось запустить сборку: HTTP ${response.code} ${response.body?.string()?.take(300)}")
            }
            // GitHub не возвращает run_id напрямую в ответ на dispatch — нужно запросить список запусков.
            ToolResult(true, "Сборка запущена (workflow=$workflowFile, ветка=$ref). Через ~10-20 секунд узнайте run_id " +
                "через GET .../actions/runs (или используйте github_action list_commits/list_pulls для контекста) " +
                "и вызовите build_apk_agent(action=check_status, run_id=...).")
        }
    }

    private fun checkStatus(owner: String, repo: String, runId: String?, token: String): ToolResult {
        if (runId.isNullOrBlank()) {
            // без run_id — покажем последние запуски, чтобы агент/пользователь выбрал нужный
            val url = "https://api.github.com/repos/$owner/$repo/actions/runs?per_page=5"
            val request = authRequest(url, token).build()
            return httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ToolResult(false, "Ошибка: HTTP ${response.code}")
                val runs = JSONObject(response.body?.string().orEmpty()).optJSONArray("workflow_runs")
                    ?: return ToolResult(true, "Запусков не найдено")
                val text = (0 until runs.length()).joinToString("\n") { i ->
                    val r = runs.getJSONObject(i)
                    "id=${r.optLong("id")} status=${r.optString("status")} conclusion=${r.optString("conclusion")}"
                }
                ToolResult(true, "Последние запуски:\n$text")
            }
        }
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId"
        val request = authRequest(url, token).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolResult(false, "Ошибка: HTTP ${response.code}")
            val obj = JSONObject(response.body?.string().orEmpty())
            val status = obj.optString("status")
            val conclusion = obj.optString("conclusion")
            val artifactsUrl = obj.optString("artifacts_url")
            ToolResult(true, "Статус: $status, результат: ${conclusion.ifBlank { "(ещё выполняется)" }}. " +
                "Артефакты (APK): $artifactsUrl (откройте в браузере или через http_request с вашим токеном)")
        }
    }
}
