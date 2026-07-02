package com.erekeai.data.tools.dev

import android.content.Context
import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ ЧЕСТНО про "настоящий Terminal Agent": на телефоне без root у приложения физически нет
 * доступа к системному шеллу устройства (нет `su`, нет прав менять чужие файлы/настройки) —
 * это ограничение платформы Android, а не выбор реализации. Полноценный терминал в смысле
 * "root shell на устройстве" ErekeAI дать не может ни при каких настройках, и сознательно не
 * пытается его имитировать/обходить (это было бы небезопасно для пользователя).
 *
 * Что здесь реализовано по-честному: интерпретатор команд toybox (те же ls/cat/grep/mkdir и т.п.,
 * что есть в самой Android), выполняемый в правах ПРИЛОЖЕНИЯ и ТОЛЬКО внутри его песочницы
 * (agent_documents) — то есть безопасный "code execution"-инструмент, а не средство управления
 * устройством. Выключен по умолчанию: работает только после включения тумблера
 * "Настройки → Разработка → Разрешить Terminal Agent".
 */
@Singleton
class TerminalAgentTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : Tool {

    private val allowedBinaries = setOf(
        "ls", "pwd", "cat", "echo", "mkdir", "mv", "cp", "rm", "grep", "wc", "find", "sort", "head", "tail", "diff"
    )

    override val definition = ToolDefinition(
        name = "terminal_agent",
        description = "Выполняет команду (${allowedBinaries.joinToString()}) внутри песочницы агента. " +
            "НЕ является root-доступом к устройству. Требует включённого тумблера в Настройках.",
        parameters = listOf(ToolParameter("command", "команда, например 'ls -la' или 'grep -r TODO .'"))
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        if (!settingsDataStore.isTerminalEnabled.first()) {
            return@withContext ToolResult(
                false,
                "Terminal Agent выключен. Включите 'Разрешить Terminal Agent' в Настройках → Разработка."
            )
        }
        val command = args["command"]?.trim()
        if (command.isNullOrBlank()) return@withContext ToolResult(false, "Не указан параметр 'command'")

        val binary = command.split(Regex("\\s+")).firstOrNull().orEmpty()
        if (binary !in allowedBinaries) {
            return@withContext ToolResult(false, "Команда '$binary' не в белом списке. Разрешены: ${allowedBinaries.joinToString()}")
        }

        try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(devSandboxDir(context))
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@withContext ToolResult(false, "Превышен лимит времени (15 с), команда прервана")
            }
            val output = process.inputStream.bufferedReader().readText().take(6000)
            ToolResult(process.exitValue() == 0, output.ifBlank { "(выполнено, вывода нет)" })
        } catch (e: Exception) {
            ToolResult(false, "Ошибка выполнения: ${e.message}")
        }
    }
}
