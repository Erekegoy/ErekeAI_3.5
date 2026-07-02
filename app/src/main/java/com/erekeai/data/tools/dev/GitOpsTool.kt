package com.erekeai.data.tools.dev

import android.content.Context
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "Git Clone/Push/Pull" — настоящие git-операции (не просто чтение через GitHub REST API,
 * как в [GitHubTool]) через JGit (org.eclipse.jgit) — чистая Java-реализация git-протокола,
 * поэтому работает на Android без системного бинаря `git` (которого на телефоне нет).
 * Репозитории живут только внутри песочницы агента (agent_documents/<dir_name>).
 * Приватные репозитории — HTTPS + Personal Access Token как пароль (см. параметр 'token';
 * можно переиспользовать тот же токен, что и в Настройках для github_action).
 *
 * action: clone (repo_url, dir_name) | pull (dir_name) | commit (dir_name, message) |
 *         push (dir_name) | status (dir_name) | checkout (dir_name, branch)
 */
@Singleton
class GitOpsTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val definition = ToolDefinition(
        name = "git_ops",
        description = "Настоящие git-операции в песочнице агента: clone, pull, commit, push, status, checkout",
        parameters = listOf(
            ToolParameter("action", "clone | pull | commit | push | status | checkout"),
            ToolParameter("dir_name", "имя папки репозитория в песочнице"),
            ToolParameter("repo_url", "URL репозитория (для clone)", required = false),
            ToolParameter("message", "сообщение коммита (для commit)", required = false),
            ToolParameter("branch", "имя ветки (для checkout)", required = false),
            ToolParameter("token", "Personal Access Token для приватных репозиториев", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        val dirName = args["dir_name"]?.trim() ?: return@withContext ToolResult(false, "Не указан 'dir_name'")
        val sandbox = devSandboxDir(context)
        val repoDir = File(sandbox, dirName)
        if (!isWithinSandbox(sandbox, repoDir)) return@withContext ToolResult(false, "Недопустимый путь")

        val creds = args["token"]?.takeIf { it.isNotBlank() }?.let { UsernamePasswordCredentialsProvider(it, "") }

        try {
            when (args["action"]?.trim()) {
                "clone" -> {
                    val repoUrl = args["repo_url"] ?: return@withContext ToolResult(false, "Не указан 'repo_url'")
                    val cmd = Git.cloneRepository().setURI(repoUrl).setDirectory(repoDir)
                    creds?.let { cmd.setCredentialsProvider(it) }
                    cmd.call().close()
                    ToolResult(true, "Репозиторий склонирован в agent_documents/$dirName")
                }
                "pull" -> Git.open(repoDir).use { git ->
                    val cmd = git.pull()
                    creds?.let { cmd.setCredentialsProvider(it) }
                    val result = cmd.call()
                    ToolResult(true, "Pull выполнен: ${result.mergeResult?.mergeStatus ?: "OK"}")
                }
                "commit" -> Git.open(repoDir).use { git ->
                    val message = args["message"] ?: return@withContext ToolResult(false, "Не указан 'message'")
                    git.add().addFilepattern(".").call()
                    val commit = git.commit().setMessage(message).call()
                    ToolResult(true, "Коммит создан: ${commit.name.take(8)} — $message")
                }
                "push" -> Git.open(repoDir).use { git ->
                    val cmd = git.push()
                    creds?.let { cmd.setCredentialsProvider(it) }
                    val results = cmd.call()
                    ToolResult(true, "Push выполнен: " + results.joinToString { it.messages.ifBlank { "OK" } })
                }
                "status" -> Git.open(repoDir).use { git ->
                    val status = git.status().call()
                    ToolResult(true, buildString {
                        appendLine("Изменённые: ${status.modified.joinToString().ifBlank { "нет" }}")
                        appendLine("Новые: ${status.untracked.joinToString().ifBlank { "нет" }}")
                        append("В индексе: ${status.added.joinToString().ifBlank { "нет" }}")
                    })
                }
                "checkout" -> Git.open(repoDir).use { git ->
                    val branch = args["branch"] ?: return@withContext ToolResult(false, "Не указан 'branch'")
                    git.checkout().setName(branch).call()
                    ToolResult(true, "Переключено на ветку '$branch'")
                }
                else -> ToolResult(false, "Не указан или неизвестен 'action'. Доступны: clone, pull, commit, push, status, checkout")
            }
        } catch (e: Exception) {
            ToolResult(false, "Ошибка git: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
