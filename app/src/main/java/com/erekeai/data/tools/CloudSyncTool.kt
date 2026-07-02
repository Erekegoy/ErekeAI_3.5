package com.erekeai.data.tools

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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "Cloud Sync" — синхронизация данных (заметок, планов, экспорта чата) с ВАШИМ собственным
 * облачным хранилищем через простой REST-контракт PUT/GET (например Firebase Realtime Database
 * REST API, ваш сервер, или сервис вроде JSONBin/Supabase). ErekeAI не привязан к конкретному
 * облаку — вы указываете полный URL эндпоинта в Настройках → Cloud Sync, а токен (если нужен)
 * передаётся как Bearer.
 *
 * action: "push" (key, content) → PUT {base_url}/{key} с телом content
 *         "pull" (key)          → GET {base_url}/{key}, возвращает содержимое
 */
@Singleton
class CloudSyncTool @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val httpClient: OkHttpClient
) : Tool {
    override val definition = ToolDefinition(
        name = "cloud_sync",
        description = "Синхронизирует данные с вашим облачным хранилищем (настраивается в Настройках → Cloud Sync): push, pull",
        parameters = listOf(
            ToolParameter("action", "push | pull"),
            ToolParameter("key", "ключ/путь записи, например 'notes/project1'"),
            ToolParameter("content", "содержимое для сохранения (для push)", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val baseUrl = secureKeyStore.getKey("cloud_sync_url")
            ?: return@withContext ToolResult(false, "Cloud Sync не настроен: укажите URL эндпоинта в Настройках → Cloud Sync")
        val token = secureKeyStore.getKey("cloud_sync_token")
        val key = args["key"]?.trim() ?: return@withContext ToolResult(false, "Не указан 'key'")
        val url = "${baseUrl.trimEnd('/')}/$key"

        try {
            when (args["action"]?.trim()) {
                "push" -> {
                    val content = args["content"] ?: return@withContext ToolResult(false, "Не указан 'content'")
                    val builder = Request.Builder().url(url).put(content.toRequestBody("application/json".toMediaType()))
                    token?.let { builder.addHeader("Authorization", "Bearer $it") }
                    httpClient.newCall(builder.build()).execute().use { response ->
                        if (response.isSuccessful) ToolResult(true, "Синхронизировано: $key") else ToolResult(false, "Ошибка синхронизации: HTTP ${response.code}")
                    }
                }
                "pull" -> {
                    val builder = Request.Builder().url(url).get()
                    token?.let { builder.addHeader("Authorization", "Bearer $it") }
                    httpClient.newCall(builder.build()).execute().use { response ->
                        if (!response.isSuccessful) return@withContext ToolResult(false, "Ошибка получения: HTTP ${response.code}")
                        ToolResult(true, response.body?.string().orEmpty().take(8000))
                    }
                }
                else -> ToolResult(false, "Не указан или неизвестен 'action'. Доступны: push, pull")
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка Cloud Sync: ${e.message}")
        }
    }
}
