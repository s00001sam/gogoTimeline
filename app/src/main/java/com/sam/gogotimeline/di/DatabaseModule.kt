package com.sam.gogotimeline.di

import android.content.Context
import androidx.room.Room
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.data.local.AppDatabase
import com.sam.gogotimeline.data.local.BoardDao
import com.sam.gogotimeline.data.local.SeedCallback
import com.sam.gogotimeline.data.local.SettingsDao
import com.sam.gogotimeline.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    // The sample is built from the device's zone and locale at the moment the file is first created.
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        clock: Clock,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
        .addCallback(
            SeedCallback(
                now = clock::millis,
                calendar = { CalendarContext.of(ZoneId.systemDefault(), Locale.getDefault()) },
            ),
        )
        .build()

    @Provides
    fun provideBoardDao(database: AppDatabase): BoardDao = database.boardDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()
}
