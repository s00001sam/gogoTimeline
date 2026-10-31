package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import java.time.LocalDate

/** One task being edited: what it was when editing started, and the unsaved draft. */
data class EditSession(
    val original: TaskEdit,
    val draft: TaskEdit,
) {
    val isDirty: Boolean get() = draft != original

    fun withSchedule(schedule: TaskSchedule): EditSession = copy(draft = draft.copy(schedule = schedule))

    /**
     * Kept in [SavedStateHandle] because losing a draft loses the user's changes when the app is killed in the
     * background. [TaskSchedule] is not storable, so each edit becomes numbers plus its title.
     */
    fun save(handle: SavedStateHandle) {
        handle[KEY_ORIGINAL] = original.encode()
        handle[KEY_DRAFT] = draft.encode()
        handle[KEY_TITLES] = arrayOf(original.title, draft.title)
    }

    companion object {
        private const val KEY_ORIGINAL = "edit.original"
        private const val KEY_DRAFT = "edit.draft"
        private const val KEY_TITLES = "edit.titles"
        private const val TIMED = 0L
        private const val ALL_DAY = 1L

        fun from(task: Task): EditSession {
            val edit = TaskEdit(task.id, task.title, task.schedule, task.colorIndex)
            return EditSession(original = edit, draft = edit)
        }

        fun restore(handle: SavedStateHandle): EditSession? {
            val original = handle.get<LongArray>(KEY_ORIGINAL) ?: return null
            val draft = handle.get<LongArray>(KEY_DRAFT) ?: return null
            val titles = handle.get<Array<String>>(KEY_TITLES) ?: return null
            return EditSession(original = decode(original, titles[0]), draft = decode(draft, titles[1]))
        }

        fun clear(handle: SavedStateHandle) {
            handle.remove<LongArray>(KEY_ORIGINAL)
            handle.remove<LongArray>(KEY_DRAFT)
            handle.remove<Array<String>>(KEY_TITLES)
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
        ): TaskEdit {
            val (taskId, colorIndex, kind, start, end) = numbers
            val schedule = if (kind == ALL_DAY) {
                TaskSchedule.AllDay(DateRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end)))
            } else {
                TaskSchedule.Timed(TimeRange(start, end))
            }
            return TaskEdit(taskId, title, schedule, colorIndex.toInt())
        }
    }
}
