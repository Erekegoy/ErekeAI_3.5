package com.erekeai.executor

import com.erekeai.approval.ApprovalDecision
import com.erekeai.approval.ApprovalRequest
import com.erekeai.approval.ApprovalService
import com.erekeai.core.FileRepository
import com.erekeai.core.Permission
import com.erekeai.diff.DiffService
import com.erekeai.planner.Planner

sealed class ExecutionResult {
    data class Success(val newContent: String) : ExecutionResult()
    data class Failed(val reason: String) : ExecutionResult()
    /** Пользователь нажал "Отклонить" — это НЕ ошибка, это осознанная остановка. */
    object CancelledByUser : ExecutionResult()
}

/**
 * Milestone 1 Executor.
 *
 * Цепочка: Read -> Planner -> Diff -> Approval -> Write.
 * Осознанно НЕТ: retry, build, git commit/push, iteration limits —
 * это добавляется в Milestone 2 поверх этого же класса, без изменения
 * его публичного контракта (extend, не rewrite).
 */
class SimpleFixExecutor(
    private val fileRepository: FileRepository,
    private val planner: Planner,
    private val diffService: DiffService,
    private val approvalService: ApprovalService
) {
    suspend fun runOnce(filePath: String, errorLog: String): ExecutionResult {
        // 1. Read
        val oldContent = fileRepository.read(filePath).getOrElse {
            return ExecutionResult.Failed("Не удалось прочитать файл $filePath: ${it.message}")
        }

        // 2. Planner
        val newContent = planner.proposeFix(filePath, oldContent, errorLog).getOrElse {
            return ExecutionResult.Failed("Planner не смог предложить исправление: ${it.message}")
        }

        if (newContent == oldContent) {
            return ExecutionResult.Failed("AI не предложил никаких изменений")
        }

        // 3. Diff
        val diffLines = diffService.diff(oldContent, newContent)

        // 4. Approval — привязан к конкретным args (path + новый контент),
        //    не к абстрактному "шагу плана".
        val decision = approvalService.request(
            ApprovalRequest(
                toolId = "file.write",
                permission = Permission.WRITE,
                title = "Изменить $filePath (${diffService.summary(diffLines)})",
                diff = diffLines
            )
        )

        if (decision == ApprovalDecision.REJECTED) {
            return ExecutionResult.CancelledByUser
        }

        // 5. Write
        return fileRepository.write(filePath, newContent).fold(
            onSuccess = { ExecutionResult.Success(newContent) },
            onFailure = { ExecutionResult.Failed("Не удалось записать файл: ${it.message}") }
        )
    }
}
