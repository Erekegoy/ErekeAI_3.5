package com.erekeai.server

import com.erekeai.domain.agent.AgentEvent
import com.erekeai.domain.agent.AgentOrchestrator
import com.erekeai.domain.agent.AgentPersona
import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.model.Role
import com.erekeai.domain.project.ProjectManager
import com.erekeai.domain.repository.ChatRepository
import com.erekeai.domain.tool.ToolRegistry
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🟡 "SDK/API, чтобы другие приложения могли использовать ErekeAI как ядро" — REST-сервер
 * (отдельный от [ErekeMcpServer], который говорит на языке протокола MCP): даёт простой JSON
 * HTTP-контракт для ЛЮБОГО клиента (не только MCP-совместимого) — другого вашего Android-
 * приложения, скрипта на ПК, веб-страницы в локальной сети.
 *
 * Эндпоинты:
 *   GET  /health                                     → {"status":"ok"}
 *   POST /agent/run    {"text":"...","provider":"gemini","persona":"general"} → финальный ответ агента
 *   GET  /tools/list                                  → список инструментов ErekeAI
 *   POST /tools/execute {"name":"web_search","args":{"query":"..."}}          → прямой вызов инструмента
 *   GET  /projects/list                               → список проектов (см. 🟡 Project Manager)
 *
 * Порт 8765 (0.0.0.0 — доступен из локальной сети). Необязательный [authToken] — заголовок
 * X-Ereke-Token. См. класс [com.erekeai.sdk.ErekeClient] в модуле :sdk — готовый Kotlin-клиент
 * для других ваших приложений, который просто оборачивает эти же эндпоинты.
 */
@Singleton
class ErekeApiServer @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val toolRegistry: ToolRegistry,
    private val projectManager: ProjectManager
) : NanoHTTPD(PORT) {

    var authToken: String? = null

    override fun serve(session: IHTTPSession): Response {
        authToken?.let { required ->
            if (session.headers["x-ereke-token"] != required) {
                return json("""{"error":"unauthorized"}""", Response.Status.UNAUTHORIZED)
            }
        }
        return try {
            when {
                session.method == Method.GET && session.uri == "/health" -> json("""{"status":"ok","name":"ErekeAI"}""")
                session.method == Method.GET && session.uri == "/tools/list" -> handleToolsList()
                session.method == Method.POST && session.uri == "/tools/execute" -> handleToolExecute(session)
                session.method == Method.POST && session.uri == "/agent/run" -> handleAgentRun(session)
                session.method == Method.GET && session.uri == "/projects/list" -> handleProjectsList()
                else -> json("""{"error":"not_found"}""", Response.Status.NOT_FOUND)
            }
        } catch (e: Exception) {
            json("""{"error":${JSONObject.quote(e.message ?: "unknown error")}}""", Response.Status.INTERNAL_ERROR)
        }
    }

    private fun handleToolsList(): Response {
        val arr = JSONArray()
        toolRegistry.all().forEach { tool -> arr.put(JSONObject().apply { put("name", tool.definition.name); put("description", tool.definition.description) }) }
        return json(arr.toString())
    }

    private fun handleToolExecute(session: IHTTPSession): Response {
        val body = JSONObject(readBody(session))
        val name = body.optString("name").ifBlank { return json("""{"error":"'name' обязателен"}""", Response.Status.BAD_REQUEST) }
        val argsObj = body.optJSONObject("args") ?: JSONObject()
        val args = argsObj.keys().asSequence().associateWith { key -> argsObj.optString(key) }
        val tool = toolRegistry.find(name) ?: return json("""{"error":"Инструмент '$name' не найден"}""", Response.Status.NOT_FOUND)
        val result = runBlocking { tool.execute(args) }
        return json(JSONObject().apply { put("success", result.success); put("content", result.content) }.toString())
    }

    private fun handleAgentRun(session: IHTTPSession): Response {
        val body = JSONObject(readBody(session))
        val text = body.optString("text").ifBlank { return json("""{"error":"'text' обязателен"}""", Response.Status.BAD_REQUEST) }
        val providerId = body.optString("provider", "gemini")
        val provider = AiProviderType.entries.firstOrNull { it.id == providerId } ?: AiProviderType.GEMINI
        val persona = if (body.optString("persona") == "developer") AgentPersona.DEVELOPER else AgentPersona.GENERAL
        val conversationId = body.optLong("conversationId", 0L)

        val reply = runBlocking {
            chatRepository.appendMessage(conversationId, Role.USER, text, provider.id)
            val history = chatRepository.getHistorySnapshot(conversationId)
            var answer = ""
            agentOrchestrator.run(history, provider, persona = persona).collect { event ->
                when (event) {
                    is AgentEvent.FinalAnswer -> { answer = event.text; chatRepository.appendMessage(conversationId, Role.ASSISTANT, event.text, provider.id) }
                    is AgentEvent.Error -> answer = "Ошибка: ${event.message}"
                    else -> {}
                }
            }
            answer
        }
        return json(JSONObject().apply { put("reply", reply) }.toString())
    }

    private fun handleProjectsList(): Response {
        val projects = runBlocking { projectManager.listProjects(includeArchived = true) }
        val arr = JSONArray()
        projects.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("description", p.description)
                put("repoOwner", p.repoOwner); put("repoName", p.repoName); put("status", p.status.name)
            })
        }
        return json(arr.toString())
    }

    private fun readBody(session: IHTTPSession): String {
        val map = HashMap<String, String>()
        session.parseBody(map)
        return map["postData"] ?: ""
    }

    private fun json(body: String, status: Response.Status = Response.Status.OK): Response =
        newFixedLengthResponse(status, "application/json", body)

    companion object { const val PORT = 8765 }
}
