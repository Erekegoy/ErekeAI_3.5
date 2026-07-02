package com.erekeai.domain.project

enum class ProjectStatus { ACTIVE, ARCHIVED }
enum class RepoProvider { NONE, GITHUB, GITLAB }

/**
 * 🟡 "Project Manager" — карточка проекта: над чем сейчас работает пользователь, к какому
 * репозиторию он привязан (для github_action/gitlab_action/build_apk_agent — не нужно каждый
 * раз указывать owner/repo вручную, если проект активен) и в каком статусе.
 */
data class Project(
    val id: Long = 0L,
    val name: String,
    val description: String,
    val repoProvider: RepoProvider = RepoProvider.NONE,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val defaultBranch: String = "main",
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)
