package com.erekeai.data.tools

import com.erekeai.domain.plugin.PluginRepository
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListAvailablePluginsTool @Inject constructor(private val pluginRepository: PluginRepository) : Tool {
    override val definition = ToolDefinition(
        "list_available_plugins", "Показывает плагины, доступные в указанном репозитории (JSON-манифест по URL)",
        listOf(ToolParameter("repo_url", "URL JSON-репозитория плагинов"))
    )
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val repoUrl = args["repo_url"] ?: return ToolResult(false, "Не указан 'repo_url'")
        return try {
            val plugins = pluginRepository.fetchAvailable(repoUrl)
            if (plugins.isEmpty()) ToolResult(true, "В репозитории нет плагинов")
            else ToolResult(true, plugins.joinToString("\n") { "- ${it.id} v${it.version}: ${it.name} — ${it.description}" })
        } catch (e: Exception) { ToolResult(false, "Ошибка получения репозитория: ${e.message}") }
    }
}

@Singleton
class InstallPluginTool @Inject constructor(private val pluginRepository: PluginRepository) : Tool {
    override val definition = ToolDefinition(
        "install_plugin", "Устанавливает плагин по id из указанного репозитория (см. list_available_plugins)",
        listOf(ToolParameter("repo_url", "URL репозитория"), ToolParameter("plugin_id", "id плагина"))
    )
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val repoUrl = args["repo_url"] ?: return ToolResult(false, "Не указан 'repo_url'")
        val pluginId = args["plugin_id"] ?: return ToolResult(false, "Не указан 'plugin_id'")
        return try {
            val manifest = pluginRepository.fetchAvailable(repoUrl).firstOrNull { it.id == pluginId }
                ?: return ToolResult(false, "Плагин '$pluginId' не найден в репозитории")
            pluginRepository.install(repoUrl, manifest)
            ToolResult(true, "Плагин '${manifest.name}' установлен как инструмент 'plugin_${manifest.id}'")
        } catch (e: Exception) { ToolResult(false, "Ошибка установки: ${e.message}") }
    }
}

@Singleton
class ListInstalledPluginsTool @Inject constructor(private val pluginRepository: PluginRepository) : Tool {
    override val definition = ToolDefinition("list_installed_plugins", "Показывает установленные плагины", emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val installed = pluginRepository.listInstalled()
        return if (installed.isEmpty()) ToolResult(true, "Установленных плагинов нет.")
        else ToolResult(true, installed.joinToString("\n") { "- ${it.manifest.id} v${it.manifest.version}: ${it.manifest.name}" })
    }
}

@Singleton
class UninstallPluginTool @Inject constructor(private val pluginRepository: PluginRepository) : Tool {
    override val definition = ToolDefinition("uninstall_plugin", "Удаляет установленный плагин по id", listOf(ToolParameter("plugin_id", "id плагина")))
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val pluginId = args["plugin_id"] ?: return ToolResult(false, "Не указан 'plugin_id'")
        pluginRepository.uninstall(pluginId)
        return ToolResult(true, "Плагин '$pluginId' удалён")
    }
}

@Singleton
class CheckPluginUpdatesTool @Inject constructor(private val pluginRepository: PluginRepository) : Tool {
    override val definition = ToolDefinition("check_plugin_updates", "Проверяет обновления для установленных плагинов", emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val updates = pluginRepository.checkUpdates()
        return if (updates.isEmpty()) ToolResult(true, "Все плагины актуальны.")
        else ToolResult(true, updates.joinToString("\n") { (installed, remote) -> "- ${installed.manifest.id}: ${installed.manifest.version} → ${remote.version} доступно" })
    }
}
