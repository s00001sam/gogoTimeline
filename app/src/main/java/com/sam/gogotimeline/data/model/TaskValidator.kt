package com.sam.gogotimeline.data.model

import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.SheetTimeEdit
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeLimits
import java.time.LocalDate

/** Domain invariants enforced again at the persistence boundary, independently of the current UI. */
object TaskValidator {
    private val firstDate = LocalDate.of(TimeConst.MIN_YEAR, 1, 1)
    private val endExclusiveLimit = LocalDate.of(TimeConst.MAX_YEAR + 1, 1, 1)

    fun requireValid(
        schedule: TaskSchedule,
        colorIndex: Int,
    ) {
        require(colorIndex in 0 until LayoutConst.TASK_COLOR_COUNT) { "Invalid task color" }
        require(SheetTimeEdit.error(schedule) == null) { "Invalid task schedule" }
        when (schedule) {
            is TaskSchedule.Timed -> require(
                schedule.range.start >= TimeLimits.MIN && schedule.range.end <= TimeLimits.MAX,
            ) { "Task time is outside the timeline" }

            is TaskSchedule.AllDay -> require(
                schedule.dates.start >= firstDate && schedule.dates.end <= endExclusiveLimit,
            ) { "Task date is outside the timeline" }
        }
    }
}
