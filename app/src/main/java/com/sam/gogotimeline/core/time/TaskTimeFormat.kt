package com.sam.gogotimeline.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One-line text for a task's time label. Always English and 24-hour, always with dates; the year
 * appears only when the range crosses years or is not in the current year.
 */
object TaskTimeFormat {
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val dateWithYearFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    /** e.g. `Oct 30 01:00 – Oct 30 02:00`; all-day `Oct 30` or `Oct 30 – Nov 1`. */
    fun barText(
        range: TimeRange,
        isAllDay: Boolean,
        now: Long,
        calendar: CalendarContext,
    ): String {
        val start = Instant.ofEpochMilli(range.start).atZone(calendar.zone)
        val end = Instant.ofEpochMilli(range.end).atZone(calendar.zone)
        val firstDay = start.toLocalDate()
        // All-day ends are exclusive, so the last shown day is the day before; never earlier than the first.
        val lastDay = if (isAllDay) maxOf(end.toLocalDate().minusDays(1), firstDay) else end.toLocalDate()
        val showYear = firstDay.year != lastDay.year || firstDay.year != calendar.localDate(now).year

        fun date(day: LocalDate): String = (if (showYear) dateWithYearFormat else dateFormat).format(day)

        fun dateTime(time: ZonedDateTime): String = "${date(time.toLocalDate())} ${timeFormat.format(time)}"

        return when {
            isAllDay && lastDay == firstDay -> date(firstDay)
            isAllDay -> "${date(firstDay)} – ${date(lastDay)}"
            else -> "${dateTime(start)} – ${dateTime(end)}"
        }
    }
}
