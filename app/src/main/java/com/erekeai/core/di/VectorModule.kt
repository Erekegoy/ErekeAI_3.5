package com.erekeai.core.di

import com.erekeai.data.embedding.GeminiEmbeddingProvider
import com.erekeai.data.embedding.OpenAiEmbeddingProvider
import com.erekeai.data.local.db.ErekeDatabase
import com.erekeai.data.local.db.VectorDao
import com.erekeai.data.vector.VectorStoreImpl
import com.erekeai.domain.vector.EmbeddingProvider
import com.erekeai.domain.vector.EmbeddingProviderRegistry
import com.erekeai.domain.vector.VectorStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VectorModule {

    @Provides
    fun provideVectorDao(db: ErekeDatabase): VectorDao = db.vectorDao()

    @Provides
    @Singleton
    fun provideEmbeddingProviderRegistry(
        gemini: GeminiEmbeddingProvider,
        openAi: OpenAiEmbeddingProvider
    ): EmbeddingProviderRegistry = EmbeddingProviderRegistry(listOf(gemini, openAi))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VectorBindModule {

    @Binds
    @Singleton
    abstract fun bindVectorStore(impl: VectorStoreImpl): VectorStore
}
