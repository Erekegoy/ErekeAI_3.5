package com.erekeai.data.agent

/**
 * Разобранный ответ модели на одном шаге ReAct-цикла.
 * Ровно одно из [action] или [finalAnswer] будет заполнено.
 */
data class ParsedStep(
    val thought: String?,
    val action: ParsedAction?,
    val finalAnswer: String?
)

data class ParsedAction(
    val toolName: String,
    val args: Map<String, String>
)

/**
 * Разбирает свободный текст модели, следующей формату ReAct:
 *
 * ```
 * Thought: ...
 * Action: имя_инструмента
 * Action Input: {"параметр": "значение"}
 * ```
 * или
 * ```
 * Thought: ...
 * Final Answer: ...
 * ```
 *
 * Формат нарочно простой (не JSON function-calling API), чтобы одинаково работать
 * с любым текстовым провайдером (Gemini/Groq/OpenAI/Offline), не полагаясь на
 * нативную поддержку function calling у каждого из них.
 */
object ReActParser {

    private val thoughtRegex = Regex("Thought:\\s*(.*?)(?=\\n(?:Action|Final Answer):|\\z)", RegexOption.DOT_MATCHES_ALL)
    private val actionRegex = Regex("Action:\\s*(.+)")
    private val actionInputRegex = Regex("Action Input:\\s*(\\{.*?\\})", RegexOption.DOT_MATCHES_ALL)
    private val finalAnswerRegex = Regex("Final Answer:\\s*(.*)", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): ParsedStep {
        val thought = thoughtRegex.find(raw)?.groupValues?.get(1)?.trim()

        val finalAnswerMatch = finalAnswerRegex.find(raw)
        if (finalAnswerMatch != null) {
            return ParsedStep(thought = thought, action = null, finalAnswer = finalAnswerMatch.groupValues[1].trim())
        }

        val actionName = actionRegex.find(raw)?.groupValues?.get(1)?.trim()
        val actionInputJson = actionInputRegex.find(raw)?.groupValues?.get(1)?.trim()

        if (actionName != null) {
            val args = actionInputJson?.let { parseSimpleJsonObject(it) } ?: emptyMap()
            return ParsedStep(thought = thought, action = ParsedAction(actionName, args), finalAnswer = null)
        }

        // Модель не следовала формату — считаем весь текст финальным ответом (fallback),
        // чтобы агент не завис в цикле на "неразобранном" ответе.
        return ParsedStep(thought = thought, action = null, finalAnswer = raw.trim())
    }

    /** Простой парсер плоского JSON-объекта вида {"key": "value", "key2": "value2"} без вложенности. */
    private fun parseSimpleJsonObject(json: String): Map<String, String> {
        return try {
            val obj = org.json.JSONObject(json)
            obj.keys().asSequence().associateWith { key -> obj.optString(key) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
