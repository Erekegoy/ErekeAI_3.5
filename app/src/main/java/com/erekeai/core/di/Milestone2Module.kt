package com.erekeai.core.di

import com.erekeai.notifier.ChangeNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Milestone2Module {

    @Provides
    @Singleton
    fun provideChangeNotifier(): ChangeNotifier =
        ChangeNotifier()
}
