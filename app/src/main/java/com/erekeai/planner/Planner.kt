package com.erekeai.planner

/**
 * Абстракция над вашим уже существующим AI-модулем (Chat / AI Router).
 *
 * ВАЖНО: не создавайте новый LLM-клиент. Реализуйте этот интерфейс
 * через ваш текущий AiRouter/ChatRepository, добавив в него (если ещё нет)
 * "raw" режим ответа без markdown-обёрток и приветствий — см. AiRouterPlanner ниже.
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

/**
 * Пример адаптера поверх гипотетического существующего AiRouter.
 * Замените AiRouter на ваш реальный класс — сигнатуру complete()/chat() и т.п.
 */
class AiRouterPlanner(
    private val aiRouter: AiRouter
) : Planner {

    override suspend fun proposeFix(
        filePath: String,
        oldContent: String,
        errorLog: String
    ): Result<String> = runCatching {
        val prompt = buildPrompt(filePath, oldContent, errorLog)
        val rawResponse = aiRouter.completeRaw(prompt)
        stripMarkdownFences(rawResponse)
    }

    private fun buildPrompt(filePath: String, oldContent: String, errorLog: String): String = """
        Ты — AI software engineer. Тебе дан файл проекта и ошибка сборки.
        Верни ИСПРАВЛЕННОЕ содержимое файла ЦЕЛИКОМ.

        Правила ответа (строго):
        - Никаких пояснений, комментариев о том, что ты сделал.
        - Никаких markdown code fences (```).
        - Только итоговый текст файла, готовый для прямой записи на диск.

        Путь файла: $filePath

        Текущее содержимое файла:
        $oldContent

        Лог ошибки сборки:
        $errorLog
    """.trimIndent()

    /**
     * Защита на случай, если модель всё-таки обернёт ответ в ```kotlin ... ```
     * несмотря на инструкцию в промпте — это встречается часто, лучше подстраховаться.
     */
    private fun stripMarkdownFences(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("```")) {
            return trimmed
                .removePrefix("```kotlin").removePrefix("```java")
                .removePrefix("```xml").removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
        return trimmed
    }
}

/**
 * Замените на ваш реальный интерфейс AI Router.
 * completeRaw() — важно, чтобы он НЕ добавлял chat-обёртки (роль ассистента,
 * markdown-форматирование, вводные фразы) поверх ответа модели.
 */
interface AiRouter {
    suspend fun completeRaw(prompt: String): String
}
