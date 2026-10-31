package com.sam.gogotimeline.data.local

import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.data.model.Task
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit as JavaTimeUnit

/** The sample board written once when the database is created (spec 13.3). Day 1 is today. */
object SampleData {
    const val BOARD_NAME = "Sample Project"

    private const val COLOR_COUNT = 7
    private const val SPRINT_DAYS = 14L

    /** Task number (1-based) to hours before now when it was last edited; others were never edited. */
    private val editedHoursAgo = mapOf(2 to 10L, 5 to 8L, 8 to 6L, 11 to 4L, 14 to 2L)

    fun tasks(
        boardId: Long,
        now: Long,
        calendar: CalendarContext,
    ): List<Task> {
        val today = calendar.localDate(now)

        // Local date plus wall time, so 09:00 stays 09:00 on a daylight-saving day.
        fun at(
            day: Int,
            hour: Int,
            minute: Int = 0,
        ): Long = today.plusDays(day - 1L).atTime(hour, minute).atZone(calendar.zone).toInstant().toEpochMilli()

        fun timed(
            start: Long,
            end: Long,
        ) = TaskSchedule.Timed(TimeRange(start, end))

        fun days(
            first: Int,
            last: Int,
        ) = TaskSchedule.AllDay(DateRange(today.plusDays(first - 1L), today.plusDays(last.toLong())))

        val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(calendar.firstDayOfWeek))
        val samples = listOf(
            "Kickoff meeting" to timed(at(1, 9), at(1, 10, 30)),
            "Write specs" to timed(at(1, 10, 30), at(2, 18)),
            "Team sync" to timed(at(2, 16), at(2, 16, 15)),
            "Build prototype" to days(2, 4),
            "Sprint 1" to TaskSchedule.AllDay(DateRange(weekStart, weekStart.plusDays(SPRINT_DAYS))),
            "Quarter planning" to days(20, 45),
            "Lunch with client" to timed(at(1, 12), at(1, 13)),
            "Design review" to timed(at(1, 14), at(1, 15, 30)),
            "Migrate database" to timed(at(0, 22), at(1, 2)),
            "Code review" to timed(at(3, 10), at(3, 11)),
            "User testing" to timed(at(4, 13), at(4, 17)),
            "Fix bugs" to days(5, 7),
            "Release v1.0" to days(8, 8),
            "Retrospective" to timed(at(9, 15), at(9, 16)),
            "Conference trip" to timed(at(12, 8), at(14, 20)),
        )
        return samples.mapIndexed { index, (title, schedule) ->
            val number = index + 1
            val createdAt = now - JavaTimeUnit.DAYS.toMillis(samples.size + 1L - number)
            Task(
                id = 0,
                boardId = boardId,
                title = title,
                schedule = schedule,
                colorIndex = index % COLOR_COUNT,
                sortOrder = index,
                createdAt = createdAt,
                updatedAt = editedHoursAgo[number]?.let { now - JavaTimeUnit.HOURS.toMillis(it) } ?: createdAt,
            )
        }
    }
}
