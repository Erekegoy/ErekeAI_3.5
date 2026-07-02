package com.erekeai.core.di

import com.erekeai.data.remote.provider.AiProviderRegistryImpl
import com.erekeai.data.repository.ChatRepositoryImpl
import com.erekeai.domain.repository.AiProviderRegistry
import com.erekeai.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAiProviderRegistry(impl: AiProviderRegistryImpl): AiProviderRegistry
}
