package com.erekeai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val repoProvider: String,
    val repoOwner: String?,
    val repoName: String?,
    val defaultBranch: String,
    val status: String,
    val createdAt: Long
)
