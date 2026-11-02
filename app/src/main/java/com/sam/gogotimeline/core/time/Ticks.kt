package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One tick; [label] is null when the label is skipped because ticks are too dense. */
data class Tick(
    val time: Long,
    val label: String?,
)

data class TimelineTicks(
    val top: List<Tick>,
    val bottom: List<Tick>,
)

/**
 * Two-tier header ticks for a time range. Labels are always English and 24-hour to match the app;
 * the week start still follows the locale through [CalendarContext].
 */
object Ticks {
    private val hourFormat = DateTimeFormatter.ofPattern("HH", Locale.ENGLISH)
    private val dayFormat = DateTimeFormatter.ofPattern("d EEE", Locale.ENGLISH)
    private val weekFormat = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    private val monthYearFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val yearFormat = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH)

    fun buildTicks(
        range: TimeRange,
        unit: TimeUnit,
        dpPerUnit: Float,
        calendar: CalendarContext,
        minLabelSpacingDp: Float,
    ): TimelineTicks = TimelineTicks(
        top = topTicks(range, unit, calendar),
        bottom = bottomTicks(range, unit, dpPerUnit, calendar, minLabelSpacingDp),
    )

    private fun bottomTicks(
        range: TimeRange,
        unit: TimeUnit,
        dpPerUnit: Float,
        calendar: CalendarContext,
        minLabelSpacingDp: Float,
    ): List<Tick> {
        val step = labelStep(unit, dpPerUnit, minLabelSpacingDp)
        val format = when (unit) {
            TimeUnit.HOUR -> return hourTicks(range, step, calendar)
            TimeUnit.DAY -> dayFormat
            TimeUnit.WEEK -> weekFormat
            TimeUnit.MONTH -> monthFormat
        }
        val axis = calendar.axis(unit)
        return (axis.indexOf(range.start)..axis.indexOf(range.end)).map { index ->
            val time = axis.startOf(index)
            val local = Instant.ofEpochMilli(time).atZone(calendar.zone)
            val label = if (Math.floorMod(index, step.toLong()) == 0L) format.format(local) else null
            Tick(time, label)
        }
    }

    /** Local hour lines, including both sides of an overlap and the first valid time after a gap. */
    private fun hourTicks(
        range: TimeRange,
        step: Int,
        calendar: CalendarContext,
    ): List<Tick> {
        val grid = calendar.grid(SnapInterval(60))
        val ticks = mutableListOf<Tick>()
        var time = grid.floor(range.start)
        while (time <= range.end) {
            val local = Instant.ofEpochMilli(time).atZone(calendar.zone)
            ticks += Tick(time, hourFormat.format(local).takeIf { local.hour % step == 0 })
            val next = grid.next(time)
            if (next <= time) break
            time = next
        }
        return ticks
    }

    /** Starts with the period containing range.start so the sticky label knows what to show. */
    private fun topTicks(
        range: TimeRange,
        unit: TimeUnit,
        calendar: CalendarContext,
    ): List<Tick> {
        val first = calendar.localDate(range.start)
        var date = when (unit) {
            TimeUnit.HOUR -> first
            TimeUnit.DAY, TimeUnit.WEEK -> first.withDayOfMonth(1)
            TimeUnit.MONTH -> first.withDayOfYear(1)
        }
        val ticks = mutableListOf<Tick>()
        while (true) {
            val time = calendar.startOfDay(date)
            if (time > range.end) break
            ticks += Tick(time, topLabel(unit, date))
            date = when (unit) {
                TimeUnit.HOUR -> date.plusDays(1)
                TimeUnit.DAY, TimeUnit.WEEK -> date.plusMonths(1)
                TimeUnit.MONTH -> date.plusYears(1)
            }
        }
        return ticks
    }

    private fun topLabel(
        unit: TimeUnit,
        date: LocalDate,
    ): String = when (unit) {
        TimeUnit.HOUR -> dateFormat
        TimeUnit.DAY, TimeUnit.WEEK -> monthYearFormat
        TimeUnit.MONTH -> yearFormat
    }.format(date)

    /** First step whose label spacing reaches [minLabelSpacingDp]; the largest step otherwise. */
    fun labelStep(
        unit: TimeUnit,
        dpPerUnit: Float,
        minLabelSpacingDp: Float,
    ): Int {
        val steps = when (unit) {
            TimeUnit.HOUR -> TimeConst.HOUR_LABEL_STEPS
            TimeUnit.DAY -> TimeConst.DAY_LABEL_STEPS
            TimeUnit.WEEK -> TimeConst.WEEK_LABEL_STEPS
            TimeUnit.MONTH -> TimeConst.MONTH_LABEL_STEPS
        }
        return steps.firstOrNull { dpPerUnit * it >= minLabelSpacingDp } ?: steps.last()
    }
}
