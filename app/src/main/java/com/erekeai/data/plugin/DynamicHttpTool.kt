package com.erekeai.data.plugin

import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.plugin.PluginManifest
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

/** Инструмент, "собранный" из декларативного [PluginManifest] — подставляет аргументы модели в URL/тело шаблона. */
class DynamicHttpTool(
    private val manifest: PluginManifest,
    private val httpClient: OkHttpClient,
    private val secureKeyStore: SecureKeyStore
) : Tool {

    override val definition = ToolDefinition(
        name = "plugin_${manifest.id}",
        description = "[Плагин v${manifest.version}] ${manifest.description}",
        parameters = manifest.parameters.map { ToolParameter(it.name, it.description, it.required) }
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        for (param in manifest.parameters) {
            if (param.required && args[param.name].isNullOrBlank()) {
                return@withContext ToolResult(false, "Не указан обязательный параметр '${param.name}'")
            }
        }
        try {
            val url = fillTemplate(manifest.urlTemplate, args)
            val builder = Request.Builder().url(url)
            manifest.authHeaderName?.let { headerName ->
                val token = manifest.authKeyId?.let { secureKeyStore.getKey(it) }
                if (!token.isNullOrBlank()) builder.addHeader(headerName, token)
            }
            if (manifest.method.equals("POST", ignoreCase = true)) {
                val body = fillTemplate(manifest.bodyTemplate.orEmpty(), args)
                builder.post(body.toRequestBody("application/json".toMediaType()))
            }
            httpClient.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty().take(6000)
                ToolResult(response.isSuccessful, text)
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка плагина '${manifest.id}': ${e.message}")
        }
    }

    private fun fillTemplate(template: String, args: Map<String, String>): String {
        var result = template
        args.forEach { (key, value) -> result = result.replace("{$key}", java.net.URLEncoder.encode(value, "UTF-8")) }
        return result
    }
}
