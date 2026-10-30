package com.sam.gogotimeline.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

/**
 * Continuous coordinate for one unit: unit index + fraction inside that unit.
 * Days, weeks and months follow local calendar boundaries, so every month has the same width.
 */
class TimeAxis(
    val unit: TimeUnit,
    private val calendar: CalendarContext,
) {
    private val weekOriginEpochDay = LocalDate.of(1970, 1, 1)
        .with(TemporalAdjusters.previousOrSame(calendar.firstDayOfWeek))
        .toEpochDay()

    // Hours use elapsed time; the shift keeps hour lines on local :00 in half-hour-offset zones.
    private val hourShiftMs = calendar.zone.rules.getStandardOffset(Instant.EPOCH).totalSeconds % 3_600 * 1_000L

    fun indexOf(time: Long): Long = when (unit) {
        TimeUnit.HOUR -> Math.floorDiv(time + hourShiftMs, HOUR_MS)
        TimeUnit.DAY -> calendar.localDate(time).toEpochDay()
        TimeUnit.WEEK -> Math.floorDiv(calendar.localDate(time).toEpochDay() - weekOriginEpochDay, 7L)
        TimeUnit.MONTH -> calendar.localDate(time).let { it.year * 12L + it.monthValue - 1 }
    }

    fun startOf(index: Long): Long = when (unit) {
        TimeUnit.HOUR -> index * HOUR_MS - hourShiftMs

        TimeUnit.DAY -> calendar.startOfDay(LocalDate.ofEpochDay(index))

        TimeUnit.WEEK -> calendar.startOfDay(LocalDate.ofEpochDay(weekOriginEpochDay + index * 7))

        TimeUnit.MONTH -> calendar.startOfDay(
            LocalDate.of(Math.floorDiv(index, 12L).toInt(), Math.floorMod(index, 12L).toInt() + 1, 1),
        )
    }

    fun position(time: Double): Double {
        val index = indexOf(floor(time).toLong())
        val start = startOf(index)
        return index + (time - start) / (startOf(index + 1) - start)
    }

    fun timeAt(position: Double): Double {
        val index = floor(position).toLong()
        val start = startOf(index)
        return start + (position - index) * (startOf(index + 1) - start)
    }

    /** Real length of the unit containing [time], e.g. 23 hours on a DST day. */
    fun unitDurationAt(time: Long): Long {
        val index = indexOf(time)
        return startOf(index + 1) - startOf(index)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
