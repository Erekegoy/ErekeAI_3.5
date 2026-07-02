package com.erekeai.domain.vector

/**
 * Контракт локальной векторной базы данных.
 *
 * Честно про реализацию (см. VectorStoreImpl): это НЕ специализированный ANN-индекс
 * (как Faiss/HNSW в "настоящих" vector DB) — поиск делается brute-force косинусным
 * сходством по всем сохранённым векторам. Для личной коллекции документов на телефоне
 * (до нескольких тысяч чанков) это быстро и абсолютно рабочее решение; для десятков
 * тысяч+ чанков потребуется настоящий ANN-индекс — это отдельная, более сложная задача.
 */
interface VectorStore {

    /** Сохраняет пачку чанков одного источника (например, всех фрагментов одного документа). */
    suspend fun upsert(chunks: List<VectorChunk>)

    /** Ищет [topK] наиболее похожих на [queryEmbedding] чанков среди чанков того же [embeddingProviderId]
     *  (сравнивать вектора из разных моделей эмбеддингов бессмысленно — разные пространства). */
    suspend fun search(queryEmbedding: FloatArray, embeddingProviderId: String, topK: Int = 5): List<VectorSearchResult>

    /** Удаляет все чанки указанного источника (например, при повторном импорте файла). */
    suspend fun deleteBySource(sourceId: String)

    /** Список источников (документов), уже проиндексированных в базе. */
    suspend fun listSources(): List<String>

    suspend fun clear()
}
