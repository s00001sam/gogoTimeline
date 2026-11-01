package com.sam.gogotimeline.data.local

import androidx.room.TypeConverter
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.model.TaskSortMode

/** Stores enums by name, so reordering an enum never misreads saved data. */
class Converters {
    @TypeConverter
    fun fromTimeUnit(unit: TimeUnit): String = unit.name

    // An unknown name (e.g. a unit removed later) falls back to Day instead of failing to load settings.
    @TypeConverter
    fun toTimeUnit(name: String): TimeUnit = TimeUnit.entries.firstOrNull { it.name == name } ?: TimeUnit.DAY

    @TypeConverter
    fun fromTaskSortMode(mode: TaskSortMode): String = mode.name

    // An unknown name falls back to the manual order, which keeps every row where the user put it.
    @TypeConverter
    fun toTaskSortMode(name: String): TaskSortMode =
        TaskSortMode.entries.firstOrNull { it.name == name } ?: TaskSortMode.MANUAL
}
