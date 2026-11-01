package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import com.sam.gogotimeline.data.model.TaskSortMode
import com.sam.gogotimeline.ui.timeline.TimelineTask

/** All-day tasks become local 00:00–00:00 in [calendar]'s zone, so a zone change redraws them on the same dates. */
internal fun Task.toTimelineTask(calendar: CalendarContext): TimelineTask {
    val range = schedule.toTimeRange(calendar)
    return TimelineTask(
        id = id,
        title = title,
        start = range.start,
        end = range.end,
        colorIndex = colorIndex,
        isAllDay = schedule is TaskSchedule.AllDay,
    )
}

/**
 * Rows in [mode]'s order (spec 9): the manual order, or by start time or by duration, earliest or shortest first. Ties
 * keep the manual order. All-day tasks count from local 00:00 in [calendar]'s zone.
 */
internal fun List<Task>.sortedFor(
    mode: TaskSortMode,
    calendar: CalendarContext,
): List<Task> = when (mode) {
    TaskSortMode.MANUAL -> sortedBy { it.sortOrder }
    TaskSortMode.START_TIME -> sortedWith(compareBy({ it.schedule.toTimeRange(calendar).start }, { it.sortOrder }))
    TaskSortMode.DURATION -> sortedWith(compareBy({ it.schedule.toTimeRange(calendar).duration }, { it.sortOrder }))
}

/** Where a board opens (spec 6.7): [time] goes at the left margin, or the center when [centered]. */
internal data class OpeningPosition(
    val rowIndex: Int,
    val time: Long,
    val centered: Boolean,
)

/** The anchor task, else the first task (e.g. the anchor was deleted), else "now" with no tasks. */
internal fun openingPosition(
    tasks: List<TimelineTask>,
    anchorTaskId: Long?,
    now: Long,
): OpeningPosition {
    if (tasks.isEmpty()) return OpeningPosition(rowIndex = 0, time = now, centered = true)
    val index = tasks.indexOfFirst { it.id == anchorTaskId }.coerceAtLeast(0)
    return OpeningPosition(rowIndex = index, time = tasks[index].start, centered = false)
}

/**
 * The tasks to draw: the database tasks with the draft's title, times, color and notes laid over its task, which is
 * moved to [draftRow] (the draft's place after reordering). A new task's draft is added, at the end unless moved.
 */
internal fun tasksWithDraft(
    tasks: List<Task>,
    draft: TaskEdit?,
    draftRow: Int? = null,
): List<Task> {
    val shown = tasksWithDraftContent(tasks, draft)
    val from = shown.indexOfFirst { it.id == draft?.taskId }
    if (draftRow == null || from < 0) return shown
    return GestureMath.moveItem(shown, from, draftRow.coerceIn(0, shown.lastIndex))
}

private fun tasksWithDraftContent(
    tasks: List<Task>,
    draft: TaskEdit?,
): List<Task> {
    if (draft == null) return tasks
    if (draft.isNew) {
        val row = Task(
            id = draft.taskId,
            boardId = tasks.firstOrNull()?.boardId ?: 0L,
            title = draft.title,
            schedule = draft.schedule,
            colorIndex = draft.colorIndex,
            sortOrder = tasks.size,
            createdAt = 0L,
            updatedAt = 0L,
            notes = draft.notes,
        )
        return tasks + row
    }
    return tasks.map { task ->
        if (task.id != draft.taskId) return@map task
        task.copy(title = draft.title, schedule = draft.schedule, colorIndex = draft.colorIndex, notes = draft.notes)
    }
}
