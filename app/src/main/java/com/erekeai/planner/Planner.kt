package com.erekeai.planner

/**
 * Абстракция над AI-модулем, который умеет предложить исправленный код файла.
 * Реальная реализация — PlannerAdapter (использует AiProviderRegistry напрямую).
 */
interface Planner {
    /**
     * Возвращает ИСПРАВЛЕННЫЙ КОД ФАЙЛА ЦЕЛИКОМ (не diff, не markdown, без пояснений).
     * Diff считается отдельно через DiffService на основе oldContent/результата.
     */
    suspend fun proposeFix(
        filePath: String,
        oldContent: String,
        errorLog: String
    ): Result<String>
}
