package com.erekeai.executor

import com.erekeai.approval.ApprovalDecision
import com.erekeai.approval.ApprovalRequest
import com.erekeai.approval.ApprovalService
import com.erekeai.backup.BackupManager
import com.erekeai.core.FileRepository
import com.erekeai.core.Permission
import com.erekeai.diff.DiffService
import com.erekeai.planner.Planner

sealed class ExecutionResult {
    data class Success(val newContent: String, val backupId: String) : ExecutionResult()
    data class Failed(val reason: String) : ExecutionResult()
    object CancelledByUser : ExecutionResult()
}

/**
 * Один проход: Read -> Backup(original) -> Planner -> Diff -> Approval -> Write.
 *
 * Backup создаётся ДО записи и содержит ОРИГИНАЛЬНОЕ содержимое файла —
 * это то, что нужно восстановить, если сборка после Write провалится
 * (см. RetryingFixExecutor).
 *
 * Осознанно нет: retry, build, git — это в RetryingFixExecutor, который
 * использует этот класс, не переписывая его.
 */
class SimpleFixExecutor(
    private val fileRepository: FileRepository,
    private val backupManager: BackupManager,
    private val planner: Planner,
    private val diffService: DiffService,
    private val approvalService: ApprovalService
) {
    /**
     * @param requireApproval если false — Diff всё равно считается (для UI/логов),
     *   но Write выполняется СРАЗУ, без ожидания решения пользователя.
     *   Используется RetryingFixExecutor начиная со 2-й попытки одной и той же задачи —
     *   пользователь уже одобрил "право чинить этот файл" на 1-й попытке.
     *   На попытке 1 всегда requireApproval = true.
     */
    suspend fun runOnce(
        filePath: String,
        errorLog: String,
        requireApproval: Boolean = true
    ): ExecutionResult {
        // 1. Read
        val oldContent = fileRepository.read(filePath).getOrElse {
            return ExecutionResult.Failed("Не удалось прочитать файл $filePath: ${it.message}")
        }

        // 2. Backup оригинала — до любых изменений
        val backupId = backupManager.backup(filePath, oldContent)

        // 3. Planner
        val newContent = planner.proposeFix(filePath, oldContent, errorLog).getOrElse {
            return ExecutionResult.Failed("Planner не смог предложить исправление: ${it.message}")
        }

        if (newContent == oldContent) {
            return ExecutionResult.Failed("AI не предложил никаких изменений")
        }

        // 4. Diff — считается всегда, даже если approval пропускается,
        //    чтобы UI/лог могли показать, что именно было изменено на этой попытке.
        val diffLines = diffService.diff(oldContent, newContent)

        // 5. Approval — только если requireApproval = true
        if (requireApproval) {
            val decision = approvalService.request(
                ApprovalRequest(
                    toolId = "file.write",
                    permission = Permission.WRITE,
                    title = "Изменить $filePath (${diffService.summary(diffLines)})",
                    diff = diffLines
                )
            )

            if (decision == ApprovalDecision.REJECTED) {
                backupManager.cleanup(backupId) // backup больше не нужен, ничего не записывали
                return ExecutionResult.CancelledByUser
            }
        }

        // 6. Write
        return fileRepository.write(filePath, newContent).fold(
            onSuccess = { ExecutionResult.Success(newContent, backupId) },
            onFailure = {
                ExecutionResult.Failed("Не удалось записать файл: ${it.message}")
            }
        )
    }
}
