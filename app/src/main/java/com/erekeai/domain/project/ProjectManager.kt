package com.erekeai.domain.project

/**
 * Управляет несколькими проектами пользователя. "Активный" проект — контекст, который
 * автоматически подмешивается в системный промпт агента (см. AgentOrchestratorImpl) и служит
 * дефолтом owner/repo для git/github/gitlab-инструментов, если они не указаны явно.
 */
interface ProjectManager {
    suspend fun createProject(
        name: String,
        description: String,
        repoProvider: RepoProvider = RepoProvider.NONE,
        repoOwner: String? = null,
        repoName: String? = null,
        defaultBranch: String = "main"
    ): Long

    suspend fun listProjects(includeArchived: Boolean = false): List<Project>
    suspend fun getProject(id: Long): Project?
    suspend fun getProjectByName(name: String): Project?
    suspend fun archiveProject(id: Long)
    suspend fun deleteProject(id: Long)

    suspend fun setActiveProject(id: Long?)
    suspend fun getActiveProject(): Project?
}
