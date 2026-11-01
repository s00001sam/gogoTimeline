package com.sam.gogotimeline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.model.TaskSortMode
import kotlinx.coroutines.flow.Flow

/** Each update touches only its own columns, so two quick changes never overwrite each other. */
@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.ID}")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.ID}")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SettingsEntity)

    @Query("UPDATE settings SET widthMultiplier = :multiplier, dpPerUnit = :dpPerUnit WHERE id = ${SettingsEntity.ID}")
    suspend fun updateWidth(
        multiplier: Float,
        dpPerUnit: Float,
    )

    @Query("UPDATE settings SET unit = :unit, dpPerUnit = :dpPerUnit WHERE id = ${SettingsEntity.ID}")
    suspend fun updateZoom(
        unit: TimeUnit,
        dpPerUnit: Float,
    )

    // Column names cannot be bound as parameters, so CASE picks the one column to change.
    @Query(
        "UPDATE settings SET " +
            "snapHourMinutes = CASE WHEN :unit = 'HOUR' THEN :minutes ELSE snapHourMinutes END, " +
            "snapDayMinutes = CASE WHEN :unit = 'DAY' THEN :minutes ELSE snapDayMinutes END, " +
            "snapWeekMinutes = CASE WHEN :unit = 'WEEK' THEN :minutes ELSE snapWeekMinutes END, " +
            "snapMonthMinutes = CASE WHEN :unit = 'MONTH' THEN :minutes ELSE snapMonthMinutes END " +
            "WHERE id = ${SettingsEntity.ID}",
    )
    suspend fun updateSnap(
        unit: TimeUnit,
        minutes: Int,
    )

    @Query("UPDATE settings SET rowHeightDp = :dp WHERE id = ${SettingsEntity.ID}")
    suspend fun updateRowHeight(dp: Float)

    @Query("UPDATE settings SET leftPaneExpanded = :expanded WHERE id = ${SettingsEntity.ID}")
    suspend fun updateLeftPaneExpanded(expanded: Boolean)

    @Query("UPDATE settings SET drawerExpanded = :expanded WHERE id = ${SettingsEntity.ID}")
    suspend fun updateDrawerExpanded(expanded: Boolean)

    @Query("UPDATE settings SET lockConfirmed = 1 WHERE id = ${SettingsEntity.ID}")
    suspend fun markLockConfirmed()

    @Query("UPDATE settings SET taskSortMode = :mode WHERE id = ${SettingsEntity.ID}")
    suspend fun updateTaskSortMode(mode: TaskSortMode)
}
