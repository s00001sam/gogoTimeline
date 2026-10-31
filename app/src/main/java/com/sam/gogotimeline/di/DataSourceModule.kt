package com.sam.gogotimeline.di

import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.datasource.BoardLocalDataSourceImpl
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSource
import com.sam.gogotimeline.data.datasource.SettingsLocalDataSourceImpl
import com.sam.gogotimeline.data.datasource.TaskLocalDataSource
import com.sam.gogotimeline.data.datasource.TaskLocalDataSourceImpl
import com.sam.gogotimeline.data.datasource.TransactionRunner
import com.sam.gogotimeline.data.datasource.TransactionRunnerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    abstract fun bindTransactionRunner(impl: TransactionRunnerImpl): TransactionRunner

    @Binds
    abstract fun bindBoardLocalDataSource(impl: BoardLocalDataSourceImpl): BoardLocalDataSource

    @Binds
    abstract fun bindTaskLocalDataSource(impl: TaskLocalDataSourceImpl): TaskLocalDataSource

    @Binds
    abstract fun bindSettingsLocalDataSource(impl: SettingsLocalDataSourceImpl): SettingsLocalDataSource
}
