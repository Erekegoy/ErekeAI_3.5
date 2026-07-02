package com.erekeai.data.local.db

import androidx.room.*

@Dao
interface WorkflowDao {
    @Insert
    suspend fun insert(workflow: WorkflowEntity): Long

    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    suspend fun getAll(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): WorkflowEntity?

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun delete(id: Long)
}
