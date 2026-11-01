package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.SheetTimeEdit
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeError
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import java.time.LocalDate

/**
 * One task being edited: what it was when editing started, and the unsaved draft, including its row ([draftRow]),
 * since reordering the task being edited is saved with ✓ (spec 6.5). The draft may hold invalid times typed in the
 * sheet; the bar shows [lastValidSchedule] instead. [timesBeforeAllDay] restores the times when All day is unchecked
 * right after checking it; it is not saved across process death (spec 7, decision 9).
 */
data class EditSession(
    val original: TaskEdit,
    val draft: TaskEdit,
    val originalRow: Int,
    val draftRow: Int = originalRow,
    val lastValidSchedule: TaskSchedule = draft.schedule,
    val timesBeforeAllDay: TaskSchedule.Timed? = null,
) {
    /** A new row counts as a change too: it is part of the draft and saved with ✓. */
    val isDirty: Boolean get() = draft != original || draftRow != originalRow

    fun withRow(row: Int): EditSession = copy(draftRow = row)

    val timeError: TimeError? get() = SheetTimeEdit.error(draft.schedule)

    /** What ✓ and Save need: a valid name and valid times. */
    val isValid: Boolean get() = TaskTitle.isValid(draft.title) && timeError == null

    /** A drag result: always valid, and it replaces any times kept for unchecking All day. */
    fun withSchedule(schedule: TaskSchedule): EditSession =
        copy(draft = draft.copy(schedule = schedule), lastValidSchedule = schedule, timesBeforeAllDay = null)

    /** A date or time picked in the sheet; may be invalid. After it, unchecking All day keeps both ends at 00:00. */
    fun withSheetSchedule(schedule: TaskSchedule): EditSession = copy(
        draft = draft.copy(schedule = schedule),
        lastValidSchedule = validOrLast(schedule),
        timesBeforeAllDay = null,
    )

    /** Checking keeps the times to restore; unchecking restores them, or leaves both ends at 00:00. */
    fun withAllDay(
        checked: Boolean,
        calendar: CalendarContext,
    ): EditSession {
        val schedule = draft.schedule
        val (allDay, kept) = when {
            checked && schedule is TaskSchedule.Timed -> SheetTimeEdit.toAllDay(schedule.range, calendar) to schedule

            !checked && schedule is TaskSchedule.AllDay ->
                (timesBeforeAllDay ?: SheetTimeEdit.fromAllDay(schedule.dates, calendar)) to null

            else -> return this
        }
        return copy(
            draft = draft.copy(schedule = allDay),
            lastValidSchedule = validOrLast(allDay),
            timesBeforeAllDay = kept,
        )
    }

    private fun validOrLast(schedule: TaskSchedule): TaskSchedule =
        if (SheetTimeEdit.error(schedule) == null) schedule else lastValidSchedule

    fun withTitle(title: String): EditSession = copy(draft = draft.copy(title = title))

    fun withColor(colorIndex: Int): EditSession = copy(draft = draft.copy(colorIndex = colorIndex))

    fun withNotes(notes: String): EditSession = copy(draft = draft.copy(notes = notes))

    /**
     * Kept in [SavedStateHandle] because losing a draft loses the user's changes when the app is killed in the
     * background. [TaskSchedule] is not storable, so each edit becomes numbers plus its title and notes.
     */
    fun save(handle: SavedStateHandle) {
        handle[KEY_ORIGINAL] = original.encode()
        handle[KEY_DRAFT] = draft.encode()
        handle[KEY_TEXTS] = arrayOf(original.title, draft.title, original.notes, draft.notes)
        handle[KEY_ROWS] = intArrayOf(originalRow, draftRow)
    }

    companion object {
        private const val KEY_ORIGINAL = "edit.original"
        private const val KEY_DRAFT = "edit.draft"
        private const val KEY_TEXTS = "edit.texts"
        private const val KEY_ROWS = "edit.rows"
        private const val TIMED = 0L
        private const val ALL_DAY = 1L

        /** [row] is the task's row when editing starts. */
        fun from(
            task: Task,
            row: Int,
        ): EditSession {
            val edit = TaskEdit(task.id, task.title, task.schedule, task.colorIndex, task.notes)
            return EditSession(original = edit, draft = edit, originalRow = row)
        }

        /**
         * A task not in the database yet, with an empty name, starting as the last row ([rowCount] rows exist);
         * unchanged it counts as not dirty and just goes away.
         */
        fun newTask(
            schedule: TaskSchedule,
            colorIndex: Int,
            rowCount: Int,
        ): EditSession {
            val edit = TaskEdit(TaskEdit.NEW_TASK_ID, title = "", schedule = schedule, colorIndex = colorIndex)
            return EditSession(original = edit, draft = edit, originalRow = rowCount)
        }

        fun restore(handle: SavedStateHandle): EditSession? {
            val original = handle.get<LongArray>(KEY_ORIGINAL) ?: return null
            val draft = handle.get<LongArray>(KEY_DRAFT) ?: return null
            val texts = handle.get<Array<String>>(KEY_TEXTS) ?: return null
            val rows = handle.get<IntArray>(KEY_ROWS) ?: return null
            val originalEdit = decode(original, title = texts[0], notes = texts[2])
            val draftEdit = decode(draft, title = texts[1], notes = texts[3])
            val draftValid = SheetTimeEdit.error(draftEdit.schedule) == null
            return EditSession(
                original = originalEdit,
                draft = draftEdit,
                originalRow = rows[0],
                draftRow = rows[1],
                lastValidSchedule = if (draftValid) draftEdit.schedule else originalEdit.schedule,
            )
        }

        fun clear(handle: SavedStateHandle) {
            handle.remove<LongArray>(KEY_ORIGINAL)
            handle.remove<LongArray>(KEY_DRAFT)
            handle.remove<Array<String>>(KEY_TEXTS)
            handle.remove<IntArray>(KEY_ROWS)
        }

        // All-day dates are stored as epoch days, so they read back as the same dates in any time zone.
        private fun TaskEdit.encode(): LongArray = when (val schedule = schedule) {
            is TaskSchedule.Timed -> longArrayOf(
                taskId,
                colorIndex.toLong(),
                TIMED,
                schedule.range.start,
                schedule.range.end,
            )

            is TaskSchedule.AllDay -> longArrayOf(
                taskId,
                colorIndex.toLong(),
                ALL_DAY,
                schedule.dates.start.toEpochDay(),
                schedule.dates.end.toEpochDay(),
            )
        }

        private fun decode(
            numbers: LongArray,
            title: String,
            notes: String,
        ): TaskEdit {
            val (taskId, colorIndex, kind, start, end) = numbers
            val schedule = if (kind == ALL_DAY) {
                TaskSchedule.AllDay(DateRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end)))
            } else {
                TaskSchedule.Timed(TimeRange(start, end))
            }
            return TaskEdit(taskId, title, schedule, colorIndex.toInt(), notes)
        }
    }
}
