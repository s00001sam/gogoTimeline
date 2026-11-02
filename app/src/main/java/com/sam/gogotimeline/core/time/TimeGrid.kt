package com.sam.gogotimeline.core.time

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Grid lines for one snap interval, in local time: sub-day intervals count minutes from local
 * midnight, 1 day snaps to local midnight, 1 week to the locale's week start.
 */
class TimeGrid(
    val interval: SnapInterval,
    private val calendar: CalendarContext,
) {
    /** Latest grid line at or before [time]. */
    fun floor(time: Long): Long {
        val local = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), calendar.zone)
        val date = local.toLocalDate()
        val minutes = interval.minutes
        return when {
            minutes >= SnapInterval.WEEK_MINUTES -> calendar.startOfDay(
                date.with(TemporalAdjusters.previousOrSame(calendar.firstDayOfWeek)),
            )

            minutes >= SnapInterval.DAY_MINUTES -> calendar.startOfDay(date)

            else -> {
                val minuteOfDay = local.hour * 60 + local.minute
                val floored = date.atStartOfDay().plusMinutes((minuteOfDay / minutes * minutes).toLong())
                // Prefer the current offset so the repeated hour at the end of DST stays in place.
                ZonedDateTime.ofLocal(floored, calendar.zone, local.offset).toInstant().toEpochMilli()
            }
        }
    }

    /** Earliest grid line at or after [time]. */
    fun ceil(time: Long): Long {
        val floor = floor(time)
        return if (floor == time) time else next(floor)
    }

    /** Closest grid line; a tie goes to the later one. */
    fun nearest(time: Long): Long {
        val floor = floor(time)
        if (floor == time) return time
        val ceil = next(floor)
        return if (time - floor < ceil - time) floor else ceil
    }

    fun isOnGrid(time: Long): Boolean = floor(time) == time

    // Steps in elapsed time and re-floors, so 23- and 25-hour DST days still land on the next line.
    internal fun next(gridTime: Long): Long {
        var candidate = gridTime + interval.millis
        while (true) {
            val floor = floor(candidate)
            if (floor > gridTime) return floor
            candidate += interval.millis
        }
    }
}
