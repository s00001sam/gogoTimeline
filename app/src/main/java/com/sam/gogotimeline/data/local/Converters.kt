package com.sam.gogotimeline.data.local

import androidx.room.TypeConverter
import com.sam.gogotimeline.core.time.TimeUnit

/** Stores [TimeUnit] by name, so reordering the enum never misreads saved data. */
class Converters {
    @TypeConverter
    fun fromTimeUnit(unit: TimeUnit): String = unit.name

    // An unknown name (e.g. a unit removed later) falls back to Day instead of failing to load settings.
    @TypeConverter
    fun toTimeUnit(name: String): TimeUnit = TimeUnit.entries.firstOrNull { it.name == name } ?: TimeUnit.DAY
}
