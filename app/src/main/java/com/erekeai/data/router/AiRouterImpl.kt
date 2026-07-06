package com.erekeai.data.router

import com.erekeai.domain.model.AiProviderType
import com.erekeai.domain.router.AiRouter
import com.erekeai.domain.router.RoutingDecision
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация "AI Router" по правилам (см. честное пояснение в интерфейсе):
 *  - есть изображение → нужен провайдер с поддержкой vision (здесь считаем, что это Gemini/OpenAI);
 *  - похоже на код/разработку (ключевые слова: код, function, bug, ошибка, рефактор, компил...) → OpenAI/Gemini;
 *  - короткий/быстрый вопрос (мало слов, нет признаков сложной задачи) → Groq (обычно самый быстрый инференс);
 *  - длинный/сложный текст (много слов, "объясни подробно", "проанализируй") → Gemini (обычно самое большое окно контекста);
 *  - иначе — первый доступный сконфигурированный провайдер по приоритету GEMINI > OPENAI > GROQ > OLLAMA.
 */
@Singleton
class AiRouterImpl @Inject constructor() : AiRouter {

    private val codeKeywords = listOf("код", "code", "function", "баг", "bug", "ошибк", "рефактор", "compile", "компил", "class ", "fun ", "import ")
    private val longFormKeywords = listOf("подробно", "проанализируй", "объясни", "сравни", "напиши статью", "детально", "explain", "analyze")

    override suspend fun route(taskText: String, hasImage: Boolean, availableProviders: List<AiProviderType>): RoutingDecision {
        val lower = taskText.lowercase()

        if (hasImage) {
            pick(availableProviders, listOf(AiProviderType.GEMINI, AiProviderType.OPENAI))
                ?.let { return RoutingDecision(it, "Задача содержит изображение — нужен провайдер с поддержкой Vision") }
        }

        if (codeKeywords.any { lower.contains(it) }) {
            pick(availableProviders, listOf(AiProviderType.OPENAI, AiProviderType.GEMINI, AiProviderType.GROQ))
                ?.let { return RoutingDecision(it, "Задача похожа на разработку/код") }
        }

        if (taskText.split(Regex("\\s+")).size <= 8 && longFormKeywords.none { lower.contains(it) }) {
            pick(availableProviders, listOf(AiProviderType.GROQ, AiProviderType.GEMINI, AiProviderType.OPENAI))
                ?.let { return RoutingDecision(it, "Короткий запрос — приоритет скорости ответа") }
        }

        if (longFormKeywords.any { lower.contains(it) } || taskText.split(Regex("\\s+")).size > 60) {
            pick(availableProviders, listOf(AiProviderType.GEMINI, AiProviderType.OPENAI, AiProviderType.GROQ))
                ?.let { return RoutingDecision(it, "Длинная/сложная задача — приоритет глубины и контекста") }
        }

        val fallback = pick(
    availableProviders,
    listOf(
        AiProviderType.OFFLINE,
        AiProviderType.GEMINI,
        AiProviderType.OPENAI,
        AiProviderType.GROQ,
        AiProviderType.OLLAMA
    )
) ?: availableProviders.firstOrNull() ?: AiProviderType.OFFLINE

return RoutingDecision(
    fallback,
    "По умолчанию используется локальная модель"
)

    private fun pick(available: List<AiProviderType>, priority: List<AiProviderType>): AiProviderType? =
        priority.firstOrNull { it in available }
}
