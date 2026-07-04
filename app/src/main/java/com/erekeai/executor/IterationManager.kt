package com.erekeai.executor

data class FixTask(val filePath: String, val errorLog: String)

data class SessionReport(
    val completed: List<Pair<FixTask, FixOutcome>>,
    val stoppedReason: String?
)

/**
 * Прогоняет очередь задач через RetryingFixExecutor с общим лимитом
 * итераций на сессию — это то самое "15-20 циклов под моим надзором",
 * а не бесконечный автономный цикл.
 *
 * Каждая задача из очереди может внутри себя сделать до maxRetries попыток
 * (см. RetryingFixExecutor), но общее количество ЗАДАЧ за сессию ограничено
 * maxIterations — после этого работа останавливается и требует явного
 * продолжения от пользователя.
 */
class IterationManager(
    private val retryingExecutor: RetryingFixExecutor,
    private val maxIterations: Int = 15
) {
    suspend fun run(tasks: List<FixTask>): SessionReport {
        val completed = mutableListOf<Pair<FixTask, FixOutcome>>()

        for ((index, task) in tasks.withIndex()) {
            if (index >= maxIterations) {
                return SessionReport(
                    completed = completed,
                    stoppedReason = "Достигнут лимит $maxIterations задач за сессию. " +
                        "Осталось ${tasks.size - index} задач в очереди — продолжите сессию вручную."
                )
            }

            val outcome = retryingExecutor.run(task.filePath, task.errorLog)
            completed.add(task to outcome)

            if (outcome is FixOutcome.CancelledByUser) {
                return SessionReport(
                    completed = completed,
                    stoppedReason = "Пользователь отклонил изменение для ${task.filePath} — сессия остановлена."
                )
            }
        }

        return SessionReport(completed = completed, stoppedReason = null)
    }
}
