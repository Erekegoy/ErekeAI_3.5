package com.erekeai.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VectorDao {

    @Insert
    suspend fun insertAll(chunks: List<VectorChunkEntity>)

    @Query("SELECT * FROM vector_chunks")
    suspend fun getAll(): List<VectorChunkEntity>

    @Query("SELECT * FROM vector_chunks WHERE embeddingProviderId = :providerId")
    suspend fun getAllByProvider(providerId: String): List<VectorChunkEntity>

    @Query("DELETE FROM vector_chunks WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT DISTINCT sourceName FROM vector_chunks ORDER BY sourceName ASC")
    suspend fun listSourceNames(): List<String>

    @Query("DELETE FROM vector_chunks")
    suspend fun clearAll()
}
