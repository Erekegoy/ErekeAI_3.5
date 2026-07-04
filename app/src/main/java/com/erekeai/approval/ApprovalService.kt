package com.erekeai.approval

import com.erekeai.core.Permission
import com.erekeai.diff.DiffLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запрос на подтверждение конкретного вызова инструмента.
 *
 * ВАЖНО (правило "no plan drift"): approval выдаётся на конкретные toolId + args,
 * а не на абстрактный шаг плана. Если Executor изменит args после получения
 * approval — нужен НОВЫЙ запрос, а не повторное использование старого.
 */
data class ApprovalRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolId: String,
    val permission: Permission,
    val title: String,
    val diff: List<DiffLine> = emptyList(),
    val plainDescription: String? = null
)

enum class ApprovalDecision { APPROVED, REJECTED }

/**
 * Приостанавливает Executor до тех пор, пока пользователь не ответит в UI.
 *
 * Использование в Executor:
 *   val decision = approvalService.request(request)
 *   if (decision == ApprovalDecision.REJECTED) return ExecutionResult.CancelledByUser
 *
 * UI подписывается на [events] и показывает ApprovalDialog на каждый новый запрос,
 * затем вызывает [resolve] с решением пользователя.
 */
@Singleton
class ApprovalService @Inject constructor() {

    private data class Pending(
        val request: ApprovalRequest,
        val continuation: (ApprovalDecision) -> Unit
    )

    private val pendingById = mutableMapOf<String, Pending>()

    private val _events = MutableSharedFlow<ApprovalRequest>(extraBufferCapacity = 4)
    val events: Flow<ApprovalRequest> = _events

    suspend fun request(request: ApprovalRequest): ApprovalDecision =
        suspendCancellableCoroutine { cont ->
            pendingById[request.id] = Pending(request) { decision ->
                if (cont.isActive) cont.resumeWith(Result.success(decision))
            }
            _events.tryEmit(request)

            cont.invokeOnCancellation {
                pendingById.remove(request.id)
            }
        }

    /** Вызывается из UI после нажатия "Одобрить"/"Отклонить". */
    fun resolve(requestId: String, decision: ApprovalDecision) {
        pendingById.remove(requestId)?.continuation?.invoke(decision)
    }
}
