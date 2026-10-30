package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Time zone and week start for all calendar math; passed in so tests are deterministic. */
data class CalendarContext(
    val zone: ZoneId,
    val firstDayOfWeek: DayOfWeek,
) {
    fun axis(unit: TimeUnit): TimeAxis = TimeAxis(unit, this)

    fun grid(interval: SnapInterval): TimeGrid = TimeGrid(interval, this)

    fun localDate(time: Long): LocalDate = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()

    // atStartOfDay handles zones where DST skips midnight.
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun isStartOfDay(time: Long): Boolean = startOfDay(localDate(time)) == time

    companion object {
        fun of(
            zone: ZoneId,
            locale: Locale,
        ): CalendarContext = CalendarContext(zone, WeekFields.of(locale).firstDayOfWeek)
    }
}
