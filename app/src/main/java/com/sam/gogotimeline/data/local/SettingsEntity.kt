package com.sam.gogotimeline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.data.model.TaskSortMode

/** The single settings row; one typed column per value so one query reads everything. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = ID,
    val unit: TimeUnit,
    val dpPerUnit: Float,
    val widthMultiplier: Float,
    val snapHourMinutes: Int,
    val snapDayMinutes: Int,
    val snapWeekMinutes: Int,
    val snapMonthMinutes: Int,
    val rowHeightDp: Float,
    val leftPaneExpanded: Boolean,
    val drawerExpanded: Boolean,
    val lockConfirmed: Boolean,
    val taskSortMode: TaskSortMode,
) {
    companion object {
        const val ID = 0
    }
}
