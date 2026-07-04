package com.erekeai.executor

import com.erekeai.approval.ApprovalDecision
import com.erekeai.approval.ApprovalRequest
import com.erekeai.approval.ApprovalService
import com.erekeai.backup.BackupManager
import com.erekeai.build.BuildRunner
import com.erekeai.core.FileRepository
import com.erekeai.core.Permission
import com.erekeai.domain.tool.Tool
import com.erekeai.domain.tool.ToolResult
import com.erekeai.diff.DiffService
import com.erekeai.notifier.ChangeNotifier
import com.erekeai.notifier.ChangeReport

/**
 * Режим подтверждения записи файла.
 *
 * EVERY_ATTEMPT        — Approval запрашивается на КАЖДУЮ попытку (самый безопасный).
 * FIRST_ATTEMPT_ONLY   — Approval только на 1-й попытке, retry идёт без диалога.
 * AUTONOMOUS_NOTIFY_ONLY — Approval на Write не запрашивается вообще; вместо этого
 *                          после каждой попытки отправляется ChangeReport через
 *                          ChangeNotifier, чтобы пользователь видел, что изменилось,
 *                          не блокируя выполнение.
 *
 * По умолчанию используется FIRST_ATTEMPT_ONLY — компромисс между контролем
 * и удобством. Смените на нужный режим при создании RetryingFixExecutor.
 */
enum class ApprovalMode {
    EVERY_ATTEMPT,
    FIRST_ATTEMPT_ONLY,
    AUTONOMOUS_NOTIFY_ONLY
}

sealed class FixOutcome {
    data class Success(val attempts: Int, val pushed: Boolean) : FixOutcome()
    data class GaveUp(val attempts: Int, val lastBuildLog: String) : FixOutcome()
    object CancelledByUser : FixOutcome()
    data class Failed(val reason: String) : FixOutcome()
}

/**
 * Полный цикл:
 * Read -> Backup(original) -> Planner -> Diff -> [Approval?] -> Write -> Build
 *   SUCCESS -> Git Approval/Autonomous -> Commit/Push -> Cleanup Backup
 *   FAIL    -> Restore Backup -> Planner(original + accumulated build log) -> Retry
 *
 * Approval на Write управляется через [approvalMode].
 * Git commit/push управляется отдельно через [gitApprovalMode] —
 * по умолчанию требует подтверждения ВСЕГДА, даже если Write работает автономно,
 * потому что push — действие с внешними последствиями (виден другим людям,
 * может триггерить CI), и это осознанно более консервативная граница, чем
 * для локальной записи файла. Меняется одним параметром конструктора, если нужно.
 */
class RetryingFixExecutor(
    private val simpleExecutor: SimpleFixExecutor,
    private val fileRepository: FileRepository,
    private val backupManager: BackupManager,
    private val diffService: DiffService,
    private val buildRunner: BuildRunner,
    private val approvalService: ApprovalService,
    private val changeNotifier: ChangeNotifier,
    private val gitTool: Tool,
    private val maxRetries: Int = 3,
    private val approvalMode: ApprovalMode = ApprovalMode.FIRST_ATTEMPT_ONLY,
    private val gitApprovalMode: ApprovalMode = ApprovalMode.EVERY_ATTEMPT
) {
    suspend fun run(filePath: String, initialErrorLog: String): FixOutcome {
        val originalContent = fileRepository.read(filePath).getOrElse {
            return FixOutcome.Failed("Не удалось прочитать файл $filePath: ${it.message}")
        }

        val accumulatedLog = StringBuilder(initialErrorLog)
        var lastBuildLog = initialErrorLog

        repeat(maxRetries) { attemptIndex ->
            val attempt = attemptIndex + 1
            val requireWriteApproval = requiresApproval(approvalMode, attempt)

            when (
                val result = simpleExecutor.runOnce(
                    filePath,
                    accumulatedLog.toString(),
                    requireWriteApproval
                )
            ) {
                is ExecutionResult.CancelledByUser -> {
                    return FixOutcome.CancelledByUser
                }
                is ExecutionResult.Failed -> {
                    return FixOutcome.Failed(result.reason)
                }
                is ExecutionResult.Success -> {
                    // В автономном режиме — сразу сообщаем, что записали, ДО результата сборки.
                    if (approvalMode == ApprovalMode.AUTONOMOUS_NOTIFY_ONLY) {
                        changeNotifier.notify(
                            ChangeReport(
                                filePath = filePath,
                                attempt = attempt,
                                diffSummary = diffService.summary(
                                    diffService.diff(originalContent, result.newContent)
                                ),
                                buildSuccess = null,
                                pushed = false,
                                message = "AI изменил $filePath (попытка $attempt), запускаю сборку..."
                            )
                        )
                    }

                    val buildResult = buildRunner.run()

                    if (buildResult.success) {
                        val pushed = requestGitApprovalAndPush(filePath, attempt)

                        changeNotifier.notify(
                            ChangeReport(
                                filePath = filePath,
                                attempt = attempt,
                                diffSummary = diffService.summary(
                                    diffService.diff(originalContent, result.newContent)
                                ),
                                buildSuccess = true,
                                pushed = pushed,
                                message = "Готово: $filePath исправлен за $attempt попытку(и), " +
                                    "сборка прошла" + if (pushed) ", изменения запушены в Git." else "."
                            )
                        )

                        backupManager.cleanup(result.backupId)
                        return FixOutcome.Success(attempts = attempt, pushed = pushed)
                    } else {
                        lastBuildLog = buildResult.log

                        val restored = backupManager.getContent(result.backupId).getOrElse {
                            return FixOutcome.Failed(
                                "Не удалось восстановить backup после неудачной сборки: ${it.message}"
                            )
                        }
                        fileRepository.write(filePath, restored)
                        backupManager.cleanup(result.backupId)

                        if (approvalMode == ApprovalMode.AUTONOMOUS_NOTIFY_ONLY) {
                            changeNotifier.notify(
                                ChangeReport(
                                    filePath = filePath,
                                    attempt = attempt,
                                    diffSummary = "откачено",
                                    buildSuccess = false,
                                    pushed = false,
                                    message = "Попытка $attempt для $filePath не прошла сборку, " +
                                        "откатил изменения, пробую снова..."
                                )
                            )
                        }

                        accumulatedLog.append(
                            "\n\n--- Попытка $attempt провалилась, сборка упала: ---\n${buildResult.log}"
                        )
                    }
                }
            }
        }

        changeNotifier.notify(
            ChangeReport(
                filePath = filePath,
                attempt = maxRetries,
                diffSummary = "не применено",
                buildSuccess = false,
                pushed = false,
                message = "Не удалось исправить $filePath за $maxRetries попыток(и). Нужна ручная проверка."
            )
        )
        return FixOutcome.GaveUp(attempts = maxRetries, lastBuildLog = lastBuildLog)
    }

    private fun requiresApproval(mode: ApprovalMode, attempt: Int): Boolean = when (mode) {
        ApprovalMode.EVERY_ATTEMPT -> true
        ApprovalMode.FIRST_ATTEMPT_ONLY -> attempt == 1
        ApprovalMode.AUTONOMOUS_NOTIFY_ONLY -> false
    }

    /**
     * Git commit/push управляется отдельным ApprovalMode (gitApprovalMode).
     * По умолчанию EVERY_ATTEMPT — push всегда требует явного клика,
     * даже если approvalMode для файлов = AUTONOMOUS_NOTIFY_ONLY.
     */
    private suspend fun requestGitApprovalAndPush(filePath: String, attempt: Int): Boolean {
        val needsApproval = when (gitApprovalMode) {
            ApprovalMode.EVERY_ATTEMPT -> true
            ApprovalMode.FIRST_ATTEMPT_ONLY -> attempt == 1
            ApprovalMode.AUTONOMOUS_NOTIFY_ONLY -> false
        }

        if (needsApproval) {
            val decision = approvalService.request(
                ApprovalRequest(
                    toolId = "git.commit_push",
                    permission = Permission.WRITE,
                    title = "Сборка прошла успешно (попытка $attempt). Закоммитить и запушить $filePath?"
                )
            )
            if (decision == ApprovalDecision.REJECTED) return false
        }

        val result = gitTool.execute(
            mapOf(
                "path" to filePath,
                "message" to "AI fix: $filePath"
            )
        )

        if (!needsApproval) {
            changeNotifier.notify(
                ChangeReport(
                    filePath = filePath,
                    attempt = attempt,
                    diffSummary = "",
                    buildSuccess = true,
                    pushed = result.success,
                    message = "Автоматически закоммичено и запушено: $filePath"
                )
            )
        }

        return result.success
    }
}
