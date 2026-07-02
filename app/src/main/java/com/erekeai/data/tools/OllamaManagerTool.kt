package com.erekeai.data.tools

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.data.remote.provider.OllamaProvider
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** ✅ "Ollama Manager" — список/загрузка моделей на сервере Ollama прямо из чата. */
@Singleton
class OllamaManagerTool @Inject constructor(
    private val ollamaProvider: OllamaProvider,
    private val settingsDataStore: SettingsDataStore
) : Tool {
    override val definition = ToolDefinition(
        name = "ollama_manager",
        description = "Управляет локальным Ollama-сервером: list_models, pull_model, set_model",
        parameters = listOf(
            ToolParameter("action", "list_models | pull_model | set_model"),
            ToolParameter("model", "имя модели (для pull_model/set_model), например llama3.2", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val baseUrl = settingsDataStore.ollamaBaseUrl.first()
        val currentModel = settingsDataStore.ollamaModel.first()
        ollamaProvider.configure(baseUrl, currentModel)

        when (args["action"]?.trim()) {
            "list_models" -> {
                val models = ollamaProvider.listModels()
                if (models.isEmpty()) ToolResult(false, "Не удалось получить список моделей. Проверьте, что Ollama запущена и доступна по $baseUrl")
                else ToolResult(true, "Модели на сервере ($baseUrl):\n" + models.joinToString("\n") { "- $it" })
            }
            "pull_model" -> {
                val model = args["model"] ?: return@withContext ToolResult(false, "Не указан 'model'")
                ToolResult(true, ollamaProvider.pullModel(model))
            }
            "set_model" -> {
                val model = args["model"] ?: return@withContext ToolResult(false, "Не указан 'model'")
                settingsDataStore.setOllamaConfig(baseUrl, model)
                ToolResult(true, "Активная модель Ollama установлена: $model")
            }
            else -> ToolResult(false, "Не указан или неизвестен 'action'. Доступны: list_models, pull_model, set_model")
        }
    }
}
