package com.sam.gogotimeline.core.time

/** A task is either timed (fixed instants) or all-day (dates that stay the same in every time zone). */
sealed interface TaskSchedule {
    /** The range to draw and drag in the current time zone. */
    fun toTimeRange(calendar: CalendarContext): TimeRange

    data class Timed(val range: TimeRange) : TaskSchedule {
        override fun toTimeRange(calendar: CalendarContext): TimeRange = range
    }

    data class AllDay(val dates: DateRange) : TaskSchedule {
        override fun toTimeRange(calendar: CalendarContext): TimeRange = TimeRange(
            calendar.startOfDay(dates.start),
            calendar.startOfDay(dates.end),
        )
    }

    companion object {
        /** Result of a drag, Save, or new task: all-day when [range] passes [TaskTimes.isAllDay]. */
        fun from(
            range: TimeRange,
            calendar: CalendarContext,
        ): TaskSchedule = if (TaskTimes.isAllDay(range, calendar)) {
            AllDay(DateRange(calendar.localDate(range.start), calendar.localDate(range.end)))
        } else {
            Timed(range)
        }
    }
}
