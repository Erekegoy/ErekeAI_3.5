package com.erekeai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Строка векторной БД: один чанк текста + его эмбеддинг, сохранённый как ByteArray
 * (4 байта на float, little-endian) — Room не умеет хранить FloatArray напрямую,
 * а TypeConverter на ByteArray проще и компактнее, чем JSON-строка чисел.
 */
@Entity(tableName = "vector_chunks")
data class VectorChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val sourceName: String,
    val chunkIndex: Int,
    val text: String,
    val embedding: ByteArray,
    val dimensions: Int,
    val embeddingProviderId: String,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorChunkEntity) return false
        return id == other.id && sourceId == other.sourceId && chunkIndex == other.chunkIndex
    }

    override fun hashCode(): Int = id.hashCode()
}
