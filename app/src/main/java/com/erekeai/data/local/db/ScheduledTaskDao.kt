package com.erekeai.data.local.db

import androidx.room.*

@Dao
interface ScheduledTaskDao {
    @Insert
    suspend fun insert(task: ScheduledTaskEntity): Long

    @Update
    suspend fun update(task: ScheduledTaskEntity)

    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<ScheduledTaskEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduledTaskEntity?

    @Query("UPDATE scheduled_tasks SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE scheduled_tasks SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun setLastRun(id: Long, timestamp: Long)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
