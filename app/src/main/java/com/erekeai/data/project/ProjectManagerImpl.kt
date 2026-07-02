package com.erekeai.data.project

import com.erekeai.data.local.datastore.SettingsDataStore
import com.erekeai.data.local.db.ProjectDao
import com.erekeai.data.local.db.ProjectEntity
import com.erekeai.domain.project.Project
import com.erekeai.domain.project.ProjectManager
import com.erekeai.domain.project.ProjectStatus
import com.erekeai.domain.project.RepoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectManagerImpl @Inject constructor(
    private val dao: ProjectDao,
    private val settingsDataStore: SettingsDataStore
) : ProjectManager {

    override suspend fun createProject(
        name: String,
        description: String,
        repoProvider: RepoProvider,
        repoOwner: String?,
        repoName: String?,
        defaultBranch: String
    ): Long = withContext(Dispatchers.IO) {
        val id = dao.insert(
            ProjectEntity(
                name = name, description = description,
                repoProvider = repoProvider.name, repoOwner = repoOwner, repoName = repoName,
                defaultBranch = defaultBranch, status = ProjectStatus.ACTIVE.name, createdAt = System.currentTimeMillis()
            )
        )
        // Первый созданный проект автоматически становится активным — удобный дефолт.
        if (settingsDataStore.activeProjectId.first() == null) setActiveProject(id)
        id
    }

    override suspend fun listProjects(includeArchived: Boolean): List<Project> = withContext(Dispatchers.IO) {
        dao.getAll(includeArchived).map { it.toDomain() }
    }

    override suspend fun getProject(id: Long): Project? = withContext(Dispatchers.IO) { dao.getById(id)?.toDomain() }

    override suspend fun getProjectByName(name: String): Project? = withContext(Dispatchers.IO) { dao.getByName(name)?.toDomain() }

    override suspend fun archiveProject(id: Long) = withContext(Dispatchers.IO) { dao.archive(id) }

    override suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
        if (settingsDataStore.activeProjectId.first() == id) settingsDataStore.setActiveProjectId(null)
    }

    override suspend fun setActiveProject(id: Long?) = settingsDataStore.setActiveProjectId(id)

    override suspend fun getActiveProject(): Project? {
        val id = settingsDataStore.activeProjectId.first() ?: return null
        return getProject(id)
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id, name = name, description = description,
        repoProvider = RepoProvider.valueOf(repoProvider), repoOwner = repoOwner, repoName = repoName,
        defaultBranch = defaultBranch, status = ProjectStatus.valueOf(status), createdAt = createdAt
    )
}
