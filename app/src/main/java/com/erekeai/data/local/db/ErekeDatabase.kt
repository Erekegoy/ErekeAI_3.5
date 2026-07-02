package com.erekeai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageEntity::class, ConversationEntity::class, VectorChunkEntity::class,
        WorkflowEntity::class, ProjectEntity::class, ScheduledTaskEntity::class
    ],
    version = 4,
    // exportSchema=false: у нас нет настроенной папки для JSON-схем и юнит-тестов миграций
    // (fallbackToDestructiveMigration() и так пересоздаёт БД при смене версии на этой стадии
    // проекта) — оставлять exportSchema=true без ksp { arg("room.schemaLocation", ...) } даёт
    // лишнее предупреждение сборки без пользы.
    exportSchema = false
)
abstract class ErekeDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun vectorDao(): VectorDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun projectDao(): ProjectDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao

    companion object {
        const val DB_NAME = "erekeai.db"
    }
}
