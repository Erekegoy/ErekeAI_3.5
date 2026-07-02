package com.erekeai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val role: String,
    val text: String,
    val imageUri: String?,
    val timestamp: Long,
    val providerId: String?
)
