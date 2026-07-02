package com.erekeai.core.di

import android.content.Context
import androidx.room.Room
import com.erekeai.data.local.db.ChatDao
import com.erekeai.data.local.db.ErekeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ErekeDatabase =
        Room.databaseBuilder(context, ErekeDatabase::class.java, ErekeDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChatDao(db: ErekeDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideWorkflowDao(db: ErekeDatabase): com.erekeai.data.local.db.WorkflowDao = db.workflowDao()

    @Provides
    fun provideProjectDao(db: ErekeDatabase): com.erekeai.data.local.db.ProjectDao = db.projectDao()

    @Provides
    fun provideScheduledTaskDao(db: ErekeDatabase): com.erekeai.data.local.db.ScheduledTaskDao = db.scheduledTaskDao()
}
