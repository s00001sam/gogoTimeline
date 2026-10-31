package com.sam.gogotimeline.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BoardEntity::class, TaskEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao

    abstract fun taskDao(): TaskDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        const val NAME = "gogo_timeline.db"
    }
}
