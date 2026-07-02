package com.erekeai.domain.model

/**
 * Поддерживаемые AI-провайдеры. Offline зарезервирован под локальную (on-device) модель.
 */
enum class AiProviderType(val id: String, val displayName: String) {
    GEMINI("gemini", "Gemini"),
    GROQ("groq", "Groq"),
    OPENAI("openai", "OpenAI"),
    OFFLINE("offline", "Offline AI"),
    /** ✅ "Ollama Manager": реальный локальный LLM через Ollama, запущенную на ПК/сервере в сети. */
    OLLAMA("ollama", "Ollama (локально)")
}
