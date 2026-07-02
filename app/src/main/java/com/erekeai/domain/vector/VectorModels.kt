package com.erekeai.domain.vector

/**
 * Один "чанк" (фрагмент) текста с его эмбеддингом — единица хранения в векторной БД.
 */
data class VectorChunk(
    val id: Long = 0,
    val sourceId: String,      // например, имя файла-источника
    val sourceName: String,    // человекочитаемое имя источника
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray,
    val embeddingProviderId: String
)

/**
 * Результат поиска по векторной БД: чанк + степень сходства (косинусное сходство, 0..1).
 */
data class VectorSearchResult(
    val chunk: VectorChunk,
    val score: Float
)
