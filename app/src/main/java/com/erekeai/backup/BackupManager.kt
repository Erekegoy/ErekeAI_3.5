package com.erekeai.backup

import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Backup(
    val id: String,
    val originalPath: String,
    val content: String,
    val createdAt: Instant
)

interface BackupManager {
    /** Сохраняет копию оригинального содержимого файла ДО записи. Возвращает backupId. */
    suspend fun backup(path: String, content: String): String

    /** Возвращает исходное содержимое файла, соответствующее backupId. */
    suspend fun getContent(backupId: String): Result<String>

    /** Удаляет backup после успешного commit/push — он больше не нужен. */
    suspend fun cleanup(backupId: String)

    /** Удаляет все backups старше maxAgeMillis — вызывать периодически, чтобы не копить мусор на устройстве. */
    suspend fun cleanupOlderThan(maxAgeMillis: Long)
}

/**
 * Хранит backups на диске в скрытой директории проекта, а не в памяти —
 * чтобы они переживали пересоздание процесса (например, если приложение
 * убито системой посреди цикла retry).
 *
 * ВАЖНО: backups — это план "Б" на случай сбоя записи/сборки.
 * Это НЕ полноценная система версионирования — для этого есть Git.
 */
class FileBackupManager(
    private val backupDir: File
) : BackupManager {

    init {
        backupDir.mkdirs()
    }

    override suspend fun backup(path: String, content: String): String = withContext(Dispatchers.IO) {
        val id = "${Instant.now().toEpochMilli()}-${path.hashCode()}"
        val meta = File(backupDir, "$id.meta").apply { writeText(path) }
        val data = File(backupDir, "$id.content").apply { writeText(content) }
        require(meta.exists() && data.exists()) { "Не удалось создать backup для $path" }
        id
    }

    override suspend fun getContent(backupId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            File(backupDir, "$backupId.content").readText()
        }
    }

    override suspend fun cleanup(backupId: String) = withContext(Dispatchers.IO) {
        File(backupDir, "$backupId.meta").delete()
        File(backupDir, "$backupId.content").delete()
        Unit
    }

    override suspend fun cleanupOlderThan(maxAgeMillis: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        backupDir.listFiles { f -> f.name.endsWith(".meta") }?.forEach { metaFile ->
            val id = metaFile.name.removeSuffix(".meta")
            val timestamp = id.substringBefore("-").toLongOrNull() ?: return@forEach
            if (now - timestamp > maxAgeMillis) {
                metaFile.delete()
                File(backupDir, "$id.content").delete()
            }
        }
        Unit
    }
}
