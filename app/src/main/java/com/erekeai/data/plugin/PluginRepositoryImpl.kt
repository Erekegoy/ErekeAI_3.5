package com.erekeai.data.plugin

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.plugin.InstalledPlugin
import com.erekeai.domain.plugin.PluginManifest
import com.erekeai.domain.plugin.PluginParameter
import com.erekeai.domain.plugin.PluginRepository
import com.erekeai.domain.tool.ToolRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pluginsDataStore by preferencesDataStore(name = "erekeai_plugins")

@Singleton
class PluginRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val secureKeyStore: SecureKeyStore,
    private val toolRegistry: ToolRegistry
) : PluginRepository {

    private val key = stringPreferencesKey("installed_json")

    override suspend fun fetchAvailable(repoUrl: String): List<PluginManifest> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(repoUrl).build()
        httpClient.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "HTTP ${response.code}" }
            val arr = JSONArray(response.body?.string().orEmpty())
            (0 until arr.length()).map { parseManifest(arr.getJSONObject(it)) }
        }
    }

    override suspend fun install(repoUrl: String, manifest: PluginManifest) = withContext(Dispatchers.IO) {
        val current = listInstalled().filterNot { it.manifest.id == manifest.id }
        val updated = current + InstalledPlugin(manifest, System.currentTimeMillis(), repoUrl)
        persist(updated)
        toolRegistry.registerDynamic(DynamicHttpTool(manifest, httpClient, secureKeyStore))
    }

    override suspend fun listInstalled(): List<InstalledPlugin> = withContext(Dispatchers.IO) {
        val raw = context.pluginsDataStore.data.first()[key] ?: "[]"
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            InstalledPlugin(
                manifest = parseManifest(obj.getJSONObject("manifest")),
                installedAt = obj.getLong("installedAt"),
                sourceRepoUrl = obj.getString("sourceRepoUrl")
            )
        }
    }

    override suspend fun uninstall(pluginId: String) = withContext(Dispatchers.IO) {
        val remaining = listInstalled().filterNot { it.manifest.id == pluginId }
        persist(remaining)
        toolRegistry.unregisterDynamic("plugin_$pluginId")
    }

    override suspend fun checkUpdates(): List<Pair<InstalledPlugin, PluginManifest>> = withContext(Dispatchers.IO) {
        val installed = listInstalled()
        val repos = installed.map { it.sourceRepoUrl }.distinct()
        val remoteByRepo = repos.associateWith { repo -> runCatching { fetchAvailable(repo) }.getOrDefault(emptyList()) }
        installed.mapNotNull { inst ->
            val remote = remoteByRepo[inst.sourceRepoUrl]?.firstOrNull { it.id == inst.manifest.id } ?: return@mapNotNull null
            if (remote.version != inst.manifest.version) inst to remote else null
        }
    }

    /** Загружает все установленные плагины как инструменты — вызывается один раз при старте приложения. */
    suspend fun restoreInstalledIntoRegistry() {
        listInstalled().forEach { installed -> toolRegistry.registerDynamic(DynamicHttpTool(installed.manifest, httpClient, secureKeyStore)) }
    }

    private suspend fun persist(list: List<InstalledPlugin>) {
        val arr = JSONArray()
        list.forEach { inst ->
            arr.put(JSONObject().apply {
                put("manifest", manifestToJson(inst.manifest))
                put("installedAt", inst.installedAt)
                put("sourceRepoUrl", inst.sourceRepoUrl)
            })
        }
        context.pluginsDataStore.edit { it[key] = arr.toString() }
    }

    private fun parseManifest(obj: JSONObject): PluginManifest {
        val paramsArr = obj.optJSONArray("parameters") ?: JSONArray()
        val params = (0 until paramsArr.length()).map { i ->
            val p = paramsArr.getJSONObject(i)
            PluginParameter(p.getString("name"), p.optString("description"), p.optBoolean("required", true))
        }
        return PluginManifest(
            id = obj.getString("id"), name = obj.getString("name"), description = obj.optString("description"),
            version = obj.optString("version", "1.0"), method = obj.optString("method", "GET"),
            urlTemplate = obj.getString("urlTemplate"), parameters = params,
            bodyTemplate = obj.optString("bodyTemplate").ifBlank { null },
            authHeaderName = obj.optString("authHeaderName").ifBlank { null },
            authKeyId = obj.optString("authKeyId").ifBlank { null }
        )
    }

    private fun manifestToJson(m: PluginManifest): JSONObject = JSONObject().apply {
        put("id", m.id); put("name", m.name); put("description", m.description); put("version", m.version)
        put("method", m.method); put("urlTemplate", m.urlTemplate)
        put("parameters", JSONArray().apply {
            m.parameters.forEach { p -> put(JSONObject().apply { put("name", p.name); put("description", p.description); put("required", p.required) }) }
        })
        m.bodyTemplate?.let { put("bodyTemplate", it) }
        m.authHeaderName?.let { put("authHeaderName", it) }
        m.authKeyId?.let { put("authKeyId", it) }
    }
}
