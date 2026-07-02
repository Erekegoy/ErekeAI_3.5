package com.erekeai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val stepsJson: String,
    val createdAt: Long
)
