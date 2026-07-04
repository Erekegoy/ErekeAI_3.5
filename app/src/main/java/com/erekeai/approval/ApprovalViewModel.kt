package com.erekeai.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Подписывается на ApprovalService.events и хранит текущий (один) запрос
 * для отображения в ApprovalHost. Если запросов приходит несколько подряд —
 * они показываются по очереди (следующий появится после resolve текущего).
 *
 * Внедрить через DI (Hilt/Koin) как singleton, использующий тот же
 * ApprovalService, который получает Executor.
 */

@HiltViewModel
class ApprovalViewModel @Inject constructor(
    private val approvalService: ApprovalService
) : ViewModel() {

    private val _currentRequest = MutableStateFlow<ApprovalRequest?>(null)
    val currentRequest: StateFlow<ApprovalRequest?> = _currentRequest

    private val queue = mutableListOf<ApprovalRequest>()

    init {
        viewModelScope.launch {
            approvalService.events.collect { request ->
                queue.add(request)
                if (_currentRequest.value == null) {
                    showNext()
                }
            }
        }
    }

    private fun showNext() {
        _currentRequest.value = queue.removeFirstOrNull()
    }

    fun respond(requestId: String, decision: ApprovalDecision) {
        approvalService.resolve(requestId, decision)
        _currentRequest.value = null
        showNext()
    }
}
