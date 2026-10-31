package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit as JavaTimeUnit

/** Why the sheet's times cannot be saved (spec 7, live validation). */
enum class TimeError {
    END_NOT_AFTER_START,
    TOO_SHORT,
}

/**
 * Date, time and All day edits made in the task sheet, plus their validation (spec 7). Dates and times are local to
 * [CalendarContext.zone]; an all-day end is shown as the stored (exclusive) end minus one day.
 */
object SheetTimeEdit {
    private const val HOURS_PER_DAY = 24
    private const val MINUTES_PER_HOUR = 60
    private val minDurationMillis = JavaTimeUnit.MINUTES.toMillis(TimeConst.SHEET_MIN_DURATION_MINUTES.toLong())

    /** The order error wins over the length error, so only one message shows. */
    fun error(schedule: TaskSchedule): TimeError? = when (schedule) {
        is TaskSchedule.Timed -> when {
            schedule.range.end <= schedule.range.start -> TimeError.END_NOT_AFTER_START
            schedule.range.duration < minDurationMillis -> TimeError.TOO_SHORT
            else -> null
        }

        is TaskSchedule.AllDay ->
            TimeError.END_NOT_AFTER_START.takeIf { schedule.dates.end <= schedule.dates.start }
    }

    fun startDate(
        schedule: TaskSchedule,
        calendar: CalendarContext,
    ): LocalDate = when (schedule) {
        is TaskSchedule.Timed -> calendar.localDate(schedule.range.start)
        is TaskSchedule.AllDay -> schedule.dates.start
    }

    fun endDate(
        schedule: TaskSchedule,
        calendar: CalendarContext,
    ): LocalDate = when (schedule) {
        is TaskSchedule.Timed -> calendar.localDate(schedule.range.end)
        is TaskSchedule.AllDay -> schedule.dates.end.minusDays(1)
    }

    /** Null for all-day tasks, which show dates only. */
    fun startTime(
        schedule: TaskSchedule,
        calendar: CalendarContext,
    ): LocalTime? = (schedule as? TaskSchedule.Timed)?.let { localTime(it.range.start, calendar) }

    fun endTime(
        schedule: TaskSchedule,
        calendar: CalendarContext,
    ): LocalTime? = (schedule as? TaskSchedule.Timed)?.let { localTime(it.range.end, calendar) }

    /** Keeps the start's time of day; the end does not move. */
    fun withStartDate(
        schedule: TaskSchedule,
        date: LocalDate,
        calendar: CalendarContext,
    ): TaskSchedule = when (schedule) {
        is TaskSchedule.Timed -> {
            val start = at(date, localTime(schedule.range.start, calendar), calendar)
            TaskSchedule.Timed(schedule.range.copy(start = start))
        }

        is TaskSchedule.AllDay -> TaskSchedule.AllDay(schedule.dates.copy(start = date))
    }

    /** [shownDate] is the date the sheet shows; an all-day end is stored one day later. */
    fun withEndDate(
        schedule: TaskSchedule,
        shownDate: LocalDate,
        calendar: CalendarContext,
    ): TaskSchedule = when (schedule) {
        is TaskSchedule.Timed -> {
            val end = at(shownDate, localTime(schedule.range.end, calendar), calendar)
            TaskSchedule.Timed(schedule.range.copy(end = end))
        }

        is TaskSchedule.AllDay -> TaskSchedule.AllDay(schedule.dates.copy(end = shownDate.plusDays(1)))
    }

    /** Changing the start never moves the end (spec 7, decision 2). All-day tasks have no times to change. */
    fun withStartTime(
        schedule: TaskSchedule,
        time: LocalTime,
        calendar: CalendarContext,
    ): TaskSchedule = when (schedule) {
        is TaskSchedule.Timed -> {
            val start = at(calendar.localDate(schedule.range.start), time, calendar)
            TaskSchedule.Timed(schedule.range.copy(start = start))
        }

        is TaskSchedule.AllDay -> schedule
    }

    fun withEndTime(
        schedule: TaskSchedule,
        time: LocalTime,
        calendar: CalendarContext,
    ): TaskSchedule = when (schedule) {
        is TaskSchedule.Timed -> {
            val end = at(calendar.localDate(schedule.range.end), time, calendar)
            TaskSchedule.Timed(schedule.range.copy(end = end))
        }

        is TaskSchedule.AllDay -> schedule
    }

    /**
     * Checking All day: the start goes back to its day's 00:00 and the end forward to the next 00:00 (an end already at
     * 00:00 stays), covering at least one day even when the times were invalid.
     */
    fun toAllDay(
        range: TimeRange,
        calendar: CalendarContext,
    ): TaskSchedule.AllDay {
        val start = calendar.localDate(range.start)
        val endDay = calendar.localDate(range.end)
        val end = if (calendar.isStartOfDay(range.end)) endDay else endDay.plusDays(1)
        return TaskSchedule.AllDay(DateRange(start, maxOf(end, start.plusDays(1))))
    }

    /** Unchecking All day without the earlier times to restore: both ends at local 00:00. */
    fun fromAllDay(
        dates: DateRange,
        calendar: CalendarContext,
    ): TaskSchedule.Timed =
        TaskSchedule.Timed(TimeRange(calendar.startOfDay(dates.start), calendar.startOfDay(dates.end)))

    /** The time picker's hour arrows: wraps 23 → 0 and 0 → 23. */
    fun stepHour(
        hour: Int,
        delta: Int,
    ): Int = Math.floorMod(hour + delta, HOURS_PER_DAY)

    /** The minute arrows move by the picker's 15-minute step and wrap 45 → 0 without changing the hour. */
    fun stepMinute(
        minute: Int,
        delta: Int,
    ): Int = Math.floorMod(minute + delta * TimeConst.PICKER_MINUTE_STEP, MINUTES_PER_HOUR)

    private fun localTime(
        time: Long,
        calendar: CalendarContext,
    ): LocalTime = Instant.ofEpochMilli(time).atZone(calendar.zone).toLocalTime()

    // Local date plus wall time, so 09:00 stays 09:00 on a daylight-saving day; a skipped time moves forward.
    private fun at(
        date: LocalDate,
        time: LocalTime,
        calendar: CalendarContext,
    ): Long = date.atTime(time).atZone(calendar.zone).toInstant().toEpochMilli()
}
