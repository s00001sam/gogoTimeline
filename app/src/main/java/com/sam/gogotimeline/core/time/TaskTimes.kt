package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst
import java.time.Instant

/** Rules for a single task's time range: the new-task default and All day. */
object TaskTimes {
    /**
     * Now floored to the current snap interval, lasting two of the current unit. Days, weeks and
     * months are added on the local calendar, so DST days and month lengths come out right.
     */
    fun newTaskRange(
        now: Long,
        unit: TimeUnit,
        grid: TimeGrid,
        calendar: CalendarContext,
    ): TimeRange {
        val start = grid.floor(now)
        val local = Instant.ofEpochMilli(start).atZone(calendar.zone)
        val count = TimeConst.NEW_TASK_UNIT_COUNT
        val end = when (unit) {
            TimeUnit.HOUR -> local.plusHours(count)
            TimeUnit.DAY -> local.plusDays(count)
            TimeUnit.WEEK -> local.plusWeeks(count)
            TimeUnit.MONTH -> local.plusMonths(count)
        }
        return TimeRange(start, end.toInstant().toEpochMilli())
    }

    /**
     * Both ends on local midnight and at least one full calendar day. Checks dates, not duration:
     * 09:00 to 09:00 the next day is not all day, while a 23-hour DST day is.
     */
    fun isAllDay(
        range: TimeRange,
        calendar: CalendarContext,
    ): Boolean = range.end > range.start &&
        calendar.isStartOfDay(range.start) &&
        calendar.isStartOfDay(range.end) &&
        calendar.localDate(range.end) > calendar.localDate(range.start)

    /** Covers every calendar date the range touches; an end already at midnight stays, since it is exclusive. */
    fun expandToAllDay(
        range: TimeRange,
        calendar: CalendarContext,
    ): TimeRange {
        val start = calendar.startOfDay(calendar.localDate(range.start))
        val end = if (calendar.isStartOfDay(range.end)) {
            range.end
        } else {
            calendar.startOfDay(calendar.localDate(range.end).plusDays(1))
        }
        return TimeRange(start, end)
    }
}
