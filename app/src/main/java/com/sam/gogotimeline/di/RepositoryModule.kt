package com.sam.gogotimeline.di

import com.sam.gogotimeline.data.repository.BoardRepository
import com.sam.gogotimeline.data.repository.BoardRepositoryImpl
import com.sam.gogotimeline.data.repository.SettingsRepository
import com.sam.gogotimeline.data.repository.SettingsRepositoryImpl
import com.sam.gogotimeline.data.repository.TaskRepository
import com.sam.gogotimeline.data.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
