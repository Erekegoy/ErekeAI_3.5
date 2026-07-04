package com.erekeai.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class BuildResult(
    val success: Boolean,
    val log: String,
    val durationMillis: Long
)

interface BuildRunner {
    suspend fun run(task: String = "assembleDebug"): BuildResult
}

/**
 * Локальный запуск ./gradlew как процесса через ProcessBuilder.
 *
 * ⚠️ ВАЖНО: это работает только в средах с доступом к обычному Linux shell
 * и установленной JVM/Gradle (например, Termux, или если ErekeAI сам
 * предоставляет такое окружение на устройстве, как AndroidIDE).
 * В "чистом" Android-приложении без такого окружения ProcessBuilder
 * скорее всего не сможет запустить gradlew — на обычных Android-устройствах
 * нет установленного JDK/Gradle, доступного стандартному приложению.
 *
 * Это ЕДИНСТВЕННЫЙ класс, который придётся заменить под ваше реальное
 * окружение сборки (например: Gradle Tooling API, удалённый CI-runner,
 * либо interop с Termux через intents/shared storage). Замена не требует
 * изменений в Executor — только новая реализация интерфейса BuildRunner.
 */
class LocalGradleBuildRunner(
    private val projectRoot: File,
    private val timeoutSeconds: Long = 180
) : BuildRunner {

    override suspend fun run(task: String): BuildResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        runCatching {
            val gradlew = File(projectRoot, "gradlew")
            require(gradlew.exists()) { "gradlew не найден в ${projectRoot.path}" }
            if (!gradlew.canExecute()) gradlew.setExecutable(true)

            val process = ProcessBuilder(gradlew.path, task, "--console=plain")
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().readText()
            val duration = System.currentTimeMillis() - startedAt

            if (!finished) {
                process.destroyForcibly()
                return@runCatching BuildResult(
                    success = false,
                    log = "$output\n\n[Таймаут сборки после $timeoutSeconds сек]",
                    durationMillis = duration
                )
            }

            BuildResult(success = process.exitValue() == 0, log = output, durationMillis = duration)
        }.getOrElse { e ->
            BuildResult(
                success = false,
                log = "Ошибка запуска сборки: ${e.message}",
                durationMillis = System.currentTimeMillis() - startedAt
            )
        }
    }
}
