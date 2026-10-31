package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
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

/** The tasks to draw: the database tasks with the draft's title, times and color laid over its task. */
internal fun tasksWithDraft(
    tasks: List<Task>,
    draft: TaskEdit?,
): List<Task> {
    if (draft == null) return tasks
    return tasks.map {
        if (it.id ==
            draft.taskId
        ) {
            it.copy(title = draft.title, schedule = draft.schedule, colorIndex = draft.colorIndex)
        } else {
            it
        }
    }
}
