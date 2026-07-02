package com.erekeai.data.tools

import com.erekeai.domain.project.ProjectManager
import com.erekeai.domain.project.RepoProvider
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolDefinition
import com.erekeai.domain.tool.ToolParameter
import com.erekeai.domain.tool.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateProjectTool @Inject constructor(private val projectManager: ProjectManager) : Tool {
    override val definition = ToolDefinition(
        name = "create_project",
        description = "Создаёт новый проект (становится активным, если активного проекта ещё нет)",
        parameters = listOf(
            ToolParameter("name", "название проекта"),
            ToolParameter("description", "краткое описание", required = false),
            ToolParameter("repo_provider", "github | gitlab | none", required = false),
            ToolParameter("repo_owner", "владелец репозитория", required = false),
            ToolParameter("repo_name", "имя репозитория", required = false),
            ToolParameter("default_branch", "ветка по умолчанию, по умолчанию main", required = false)
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val name = args["name"]?.trim() ?: return ToolResult(false, "Не указан 'name'")
        val provider = try {
            RepoProvider.valueOf((args["repo_provider"] ?: "none").uppercase())
        } catch (e: Exception) { RepoProvider.NONE }
        val id = projectManager.createProject(
            name = name,
            description = args["description"].orEmpty(),
            repoProvider = provider,
            repoOwner = args["repo_owner"],
            repoName = args["repo_name"],
            defaultBranch = args["default_branch"] ?: "main"
        )
        return ToolResult(true, "Проект '$name' создан (id=$id).")
    }
}

@Singleton
class ListProjectsTool @Inject constructor(private val projectManager: ProjectManager) : Tool {
    override val definition = ToolDefinition("list_projects", "Показывает список проектов пользователя", emptyList())
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val active = projectManager.getActiveProject()
        val projects = projectManager.listProjects()
        if (projects.isEmpty()) return ToolResult(true, "Проектов пока нет. Создайте через create_project.")
        return ToolResult(true, projects.joinToString("\n") { p ->
            val marker = if (p.id == active?.id) "⭐ " else "- "
            "$marker${p.name} (id=${p.id})${if (p.repoOwner != null) ", repo: ${p.repoOwner}/${p.repoName}" else ""}"
        })
    }
}

@Singleton
class SetActiveProjectTool @Inject constructor(private val projectManager: ProjectManager) : Tool {
    override val definition = ToolDefinition(
        "set_active_project", "Делает проект активным — его контекст (в т.ч. репозиторий) будет автоматически подмешиваться агенту",
        listOf(ToolParameter("project_id", "id проекта из list_projects"))
    )
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val id = args["project_id"]?.toLongOrNull() ?: return ToolResult(false, "Не указан или некорректен 'project_id'")
        val project = projectManager.getProject(id) ?: return ToolResult(false, "Проект с id=$id не найден")
        projectManager.setActiveProject(id)
        return ToolResult(true, "Активный проект: ${project.name}")
    }
}

@Singleton
class ArchiveProjectTool @Inject constructor(private val projectManager: ProjectManager) : Tool {
    override val definition = ToolDefinition("archive_project", "Архивирует проект по id", listOf(ToolParameter("project_id", "id проекта")))
    override suspend fun execute(args: Map<String, String>): ToolResult {
        val id = args["project_id"]?.toLongOrNull() ?: return ToolResult(false, "Не указан или некорректен 'project_id'")
        projectManager.archiveProject(id)
        return ToolResult(true, "Проект id=$id архивирован")
    }
}
