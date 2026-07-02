package com.erekeai.data.local.db

import androidx.room.*

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE status = 'ACTIVE' OR :includeArchived ORDER BY createdAt DESC")
    suspend fun getAll(includeArchived: Boolean): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ProjectEntity?

    @Query("UPDATE projects SET status = 'ARCHIVED' WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
}
