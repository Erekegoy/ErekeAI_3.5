package com.erekeai.core

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

/**
 * Единый слой доступа к файлам проекта.
 *
 * Правило: никто в проекте не вызывает File(...).writeText()/readText() напрямую.
 * Все операции чтения/записи файлов проекта проходят через этот интерфейс —
 * это даёт единую точку для будущих hook-ов: логирование в Memory,
 * блокировка конкурентного доступа, отслеживание внешних изменений (Git checkout и т.п.).
 */
interface FileRepository {
    suspend fun read(path: String): Result<String>
    suspend fun write(path: String, content: String): Result<Unit>
    suspend fun exists(path: String): Boolean
}

/**
 * Реализация на основе локальной файловой системы Android.
 *
 * @param projectRoot корневая директория текущего открытого проекта.
 *   Все переданные пути (path) считаются относительными к этому корню.
 */
class LocalFileRepository(
    private val projectRoot: File
) : FileRepository {

    // Простая защита от одновременной записи в один и тот же файл
    // из разных корутин (автосохранение + AI Agent + Git).
    private val locks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()

    private fun lockFor(path: String) =
        locks.computeIfAbsent(path) { kotlinx.coroutines.sync.Mutex() }

    private fun resolve(path: String): File {
        val target = File(projectRoot, path).canonicalFile
        val root = projectRoot.canonicalFile
        // Защита от path traversal (../../etc/passwd), т.к. пути могут прийти от LLM.
        require(target.path.startsWith(root.path)) {
            "Path escapes project root: $path"
        }
        return target
    }

    override suspend fun read(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            resolve(path).readText()
        }
    }

    override suspend fun write(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mutex = lockFor(path)
        mutex.withLock {
            runCatching {
                val file = resolve(path)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { resolve(path).exists() }.getOrDefault(false)
    }
}
