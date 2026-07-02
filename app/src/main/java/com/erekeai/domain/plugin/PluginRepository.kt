package com.erekeai.domain.plugin

/**
 * 🟡 "Система установки и обновления плагинов из репозитория" — репозиторий это обычный
 * статический JSON-файл (массив [PluginManifest]) на любом URL (GitHub raw, ваш сервер, gist).
 */
interface PluginRepository {
    suspend fun fetchAvailable(repoUrl: String): List<PluginManifest>
    suspend fun install(repoUrl: String, manifest: PluginManifest)
    suspend fun listInstalled(): List<InstalledPlugin>
    suspend fun uninstall(pluginId: String)
    /** Сверяет версии установленных плагинов с их исходными репозиториями. */
    suspend fun checkUpdates(): List<Pair<InstalledPlugin, PluginManifest>>
}
