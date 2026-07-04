package com.erekeai.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Отчёт о том, что было сделано с файлом на конкретной попытке.
 * Используется в AUTONOMOUS-режиме, где Write выполняется без Approval,
 * но пользователь ДОЛЖЕН узнать, что именно изменилось.
 */
data class ChangeReport(
    val filePath: String,
    val attempt: Int,
    val diffSummary: String,
    val buildSuccess: Boolean?,   // null если сборка ещё не запускалась на этом шаге
    val pushed: Boolean,
    val message: String
)

/**
 * Канал уведомлений о действиях AI. UI подписывается на [reports] и показывает
 * их как ленту/историю (Snackbar, список в экране AI Agent, системное
 * уведомление Android — на ваш выбор реализации).
 */
class ChangeNotifier {
    private val _reports = MutableSharedFlow<ChangeReport>(
        replay = 20, // держим последние 20 отчётов, чтобы UI не терял историю при пересоздании экрана
        extraBufferCapacity = 20
    )
    val reports: Flow<ChangeReport> = _reports

    fun notify(report: ChangeReport) {
        _reports.tryEmit(report)
    }
}
