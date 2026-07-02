package com.erekeai.data.vector

import com.erekeai.data.local.db.VectorChunkEntity
import com.erekeai.data.local.db.VectorDao
import com.erekeai.domain.vector.VectorChunk
import com.erekeai.domain.vector.VectorSearchResult
import com.erekeai.domain.vector.VectorStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Room-backed векторная БД с brute-force косинусным поиском (см. пояснение
 * в VectorStore.kt про ограничения масштаба). Реально работает и достаточно
 * для личного использования: сотни-тысячи чанков ищутся за миллисекунды на
 * современном телефоне.
 */
@Singleton
class VectorStoreImpl @Inject constructor(
    private val dao: VectorDao
) : VectorStore {

    override suspend fun upsert(chunks: List<VectorChunk>) = withContext(Dispatchers.IO) {
        val entities = chunks.map { chunk ->
            VectorChunkEntity(
                sourceId = chunk.sourceId,
                sourceName = chunk.sourceName,
                chunkIndex = chunk.chunkIndex,
                text = chunk.text,
                embedding = floatArrayToBytes(chunk.embedding),
                dimensions = chunk.embedding.size,
                embeddingProviderId = chunk.embeddingProviderId,
                createdAt = System.currentTimeMillis()
            )
        }
        dao.insertAll(entities)
    }

    override suspend fun search(
        queryEmbedding: FloatArray,
        embeddingProviderId: String,
        topK: Int
    ): List<VectorSearchResult> =
        withContext(Dispatchers.Default) {
            val all = dao.getAllByProvider(embeddingProviderId)
            all.asSequence()
                .map { entity ->
                    val vector = bytesToFloatArray(entity.embedding)
                    val score = cosineSimilarity(queryEmbedding, vector)
                    VectorSearchResult(
                        chunk = VectorChunk(
                            id = entity.id,
                            sourceId = entity.sourceId,
                            sourceName = entity.sourceName,
                            chunkIndex = entity.chunkIndex,
                            text = entity.text,
                            embedding = vector,
                            embeddingProviderId = entity.embeddingProviderId
                        ),
                        score = score
                    )
                }
                .sortedByDescending { it.score }
                .take(topK)
                .toList()
        }

    override suspend fun deleteBySource(sourceId: String) = withContext(Dispatchers.IO) {
        dao.deleteBySource(sourceId)
    }

    override suspend fun listSources(): List<String> = withContext(Dispatchers.IO) {
        dao.listSourceNames()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return -1f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    private fun floatArrayToBytes(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = buffer.getFloat()
        return floats
    }
}
