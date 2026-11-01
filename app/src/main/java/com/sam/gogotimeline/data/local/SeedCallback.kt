package com.sam.gogotimeline.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.data.model.TimelineSettings

/**
 * Writes the sample board, its tasks and default settings once, when the database file is created.
 * This runs inside the creation transaction, so a failure leaves no half-written data and the first
 * query always sees the full sample. DAOs are not available yet, hence the raw inserts.
 */
class SeedCallback(
    private val now: () -> Long,
    private val calendar: () -> CalendarContext,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val time = now()
        val boardId = db.insert(
            "boards",
            SQLiteDatabase.CONFLICT_ABORT,
            BoardEntity(name = SampleData.BOARD_NAME, anchorTaskId = null, lastOpenedAt = time).toValues(),
        )
        val taskIds = SampleData.tasks(boardId, time, calendar()).map {
            db.insert("tasks", SQLiteDatabase.CONFLICT_ABORT, it.toEntity().toValues())
        }
        db.update(
            "boards",
            SQLiteDatabase.CONFLICT_ABORT,
            ContentValues().apply { put("anchorTaskId", taskIds.first()) },
            "id = ?",
            arrayOf(boardId),
        )
        db.insert("settings", SQLiteDatabase.CONFLICT_ABORT, TimelineSettings.DEFAULT.toEntity().toValues())
    }
}

// Generated ids are left out; putting 0 would store 0 as the id.
private fun BoardEntity.toValues() = ContentValues().apply {
    put("name", name)
    put("anchorTaskId", anchorTaskId)
    put("lastOpenedAt", lastOpenedAt)
    put("locked", if (locked) 1 else 0)
}

private fun TaskEntity.toValues() = ContentValues().apply {
    put("boardId", boardId)
    put("title", title)
    put("notes", notes)
    put("startTime", startTime)
    put("endTime", endTime)
    put("colorIndex", colorIndex)
    put("isAllDay", if (isAllDay) 1 else 0)
    put("sortOrder", sortOrder)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}

private fun SettingsEntity.toValues() = ContentValues().apply {
    put("id", id)
    put("unit", Converters().fromTimeUnit(unit))
    put("dpPerUnit", dpPerUnit)
    put("widthMultiplier", widthMultiplier)
    put("snapHourMinutes", snapHourMinutes)
    put("snapDayMinutes", snapDayMinutes)
    put("snapWeekMinutes", snapWeekMinutes)
    put("snapMonthMinutes", snapMonthMinutes)
    put("rowHeightDp", rowHeightDp)
    put("leftPaneExpanded", if (leftPaneExpanded) 1 else 0)
    put("drawerExpanded", if (drawerExpanded) 1 else 0)
    put("lockConfirmed", if (lockConfirmed) 1 else 0)
    put("taskSortMode", Converters().fromTaskSortMode(taskSortMode))
}
