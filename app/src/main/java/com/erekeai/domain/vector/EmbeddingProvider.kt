package com.erekeai.domain.vector

/**
 * Контракт провайдера эмбеддингов — превращает текст в числовой вектор.
 *
 * Честно: полноценной бесплатной оффлайн-модели эмбеддингов в проекте нет — это
 * отдельная (немаленькая) ML-модель, которую пришлось бы отдельно встраивать
 * (как Gemma для LOCAL_LLM). Сейчас эмбеддинги считаются через облачные API
 * (Gemini/OpenAI) — тот же принцип "нужен свой ключ", что и для самого чата.
 */
interface EmbeddingProvider {

    val id: String
    val dimensions: Int

    fun isConfigured(): Boolean

    suspend fun embed(text: String): FloatArray
}

/**
 * Возвращает первый настроенный (есть API-ключ) провайдер эмбеддингов, или null,
 * если ни один ключ не задан — тогда RAG/векторный поиск честно недоступны.
 */
class EmbeddingProviderRegistry(private val providers: List<EmbeddingProvider>) {
    fun resolveConfigured(): EmbeddingProvider? = providers.firstOrNull { it.isConfigured() }
    fun all(): List<EmbeddingProvider> = providers
}
