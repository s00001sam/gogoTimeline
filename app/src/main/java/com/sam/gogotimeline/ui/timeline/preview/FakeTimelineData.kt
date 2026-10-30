package com.sam.gogotimeline.ui.timeline.preview

import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.ui.timeline.TimelineTask
import java.time.LocalDate
import java.time.LocalTime

/**
 * About ten tasks around today covering every case the scheduler draws: short, multi-day, multi-week,
 * across midnight, all-day, and a long title. Used by the temporary step-3 screen and by previews.
 */
object FakeTimelineData {
    fun tasks(
        now: Long,
        calendar: CalendarContext,
    ): List<TimelineTask> {
        val today = calendar.localDate(now)

        fun at(
            day: LocalDate,
            time: String,
        ): Long = day.atTime(LocalTime.parse(time)).atZone(calendar.zone).toInstant().toEpochMilli()

        fun timed(
            id: Long,
            title: String,
            colorIndex: Int,
            start: Long,
            end: Long,
        ) = TimelineTask(id, title, start, end, colorIndex, isAllDay = false)

        fun allDay(
            id: Long,
            title: String,
            colorIndex: Int,
            firstDay: LocalDate,
            days: Long,
        ) = TimelineTask(
            id,
            title,
            calendar.startOfDay(firstDay),
            calendar.startOfDay(firstDay.plusDays(days)),
            colorIndex,
            isAllDay = true,
        )

        return listOf(
            timed(1, "Design review", 4, at(today, "09:00"), at(today, "10:30")),
            timed(2, "Standup", 1, at(today, "10:00"), at(today, "10:15")),
            timed(3, "Write tests", 3, at(today, "13:00"), at(today, "17:00")),
            timed(4, "Gym", 2, at(today, "18:30"), at(today, "19:30")),
            timed(5, "Migrate database", 5, at(today.minusDays(1), "22:00"), at(today, "02:00")),
            allDay(6, "Release day", 0, today.plusDays(1), 1),
            timed(
                7,
                "Quarterly planning with the whole product and design team",
                6,
                at(today.plusDays(1), "14:00"),
                at(today.plusDays(1), "16:00"),
            ),
            timed(8, "Conference trip", 4, at(today.plusDays(2), "08:00"), at(today.plusDays(4), "20:00")),
            allDay(9, "Sprint", 3, today.minusDays(2), 15),
            allDay(10, "Holiday", 2, today.plusDays(7), 5),
        )
    }
}
