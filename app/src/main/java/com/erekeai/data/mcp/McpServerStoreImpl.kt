package com.erekeai.data.mcp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erekeai.core.security.SecureKeyStore
import com.erekeai.domain.mcp.McpServerConfig
import com.erekeai.domain.mcp.McpServerStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mcpDataStore by preferencesDataStore(name = "erekeai_mcp_servers")

@Singleton
class McpServerStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyStore: SecureKeyStore
) : McpServerStore {
    private val key = stringPreferencesKey("servers_json")

    override suspend fun getServers(): List<McpServerConfig> {
        val raw = context.mcpDataStore.data.first()[key] ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i); val id = obj.getString("id")
            McpServerConfig(id, obj.optString("name", id), obj.getString("url"), secureKeyStore.getKey("mcp_token_$id"))
        }
    }

    override suspend fun saveServer(config: McpServerConfig) {
        val current = getServers().filterNot { it.id == config.id }
        val arr = JSONArray()
        (current + config).forEach { c -> arr.put(JSONObject().apply { put("id", c.id); put("name", c.name); put("url", c.url) }) }
        context.mcpDataStore.edit { it[key] = arr.toString() }
        config.bearerToken?.let { secureKeyStore.saveKey("mcp_token_${config.id}", it) }
    }

    override suspend fun deleteServer(id: String) {
        val remaining = getServers().filterNot { it.id == id }
        val arr = JSONArray()
        remaining.forEach { c -> arr.put(JSONObject().apply { put("id", c.id); put("name", c.name); put("url", c.url) }) }
        context.mcpDataStore.edit { it[key] = arr.toString() }
        secureKeyStore.clearKey("mcp_token_$id")
    }
}
