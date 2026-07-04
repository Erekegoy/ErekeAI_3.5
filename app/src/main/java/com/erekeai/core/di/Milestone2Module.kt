package com.erekeai.core.di

import com.erekeai.backup.BackupManager
import com.erekeai.backup.FileBackupManager
import com.erekeai.build.BuildRunner
import com.erekeai.build.NoOpBuildRunner
import com.erekeai.notifier.ChangeNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Milestone2Module {

    @Provides
    @Singleton
    fun provideBackupManager(): BackupManager =
        FileBackupManager(File(".erekeai-backups"))

    @Provides
    @Singleton
    fun provideBuildRunner(): BuildRunner =
        NoOpBuildRunner()

    @Provides
    @Singleton
    fun provideChangeNotifier(): ChangeNotifier =
        ChangeNotifier()
}
