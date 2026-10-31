package com.sam.gogotimeline.data.local

import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TimelineSettings
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappersTest {
    private val createdAt = TAIPEI.at("2026-10-16T09:00")
    private val updatedAt = TAIPEI.at("2026-10-30T15:00")

    private fun task(schedule: TaskSchedule) = Task(
        id = 7,
        boardId = 1,
        title = "Lunch with client",
        schedule = schedule,
        colorIndex = 6,
        sortOrder = 6,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private val releaseDay = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)))

    private fun utcMidnight(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun timedTaskSurvivesARoundTrip() {
        val original = task(TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30"))))
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun allDayTaskIsStoredAsUtcMidnight() {
        val entity = task(releaseDay).toEntity()
        assertTrue(entity.isAllDay)
        assertEquals(utcMidnight(LocalDate.of(2026, 11, 7)), entity.startTime)
        assertEquals(utcMidnight(LocalDate.of(2026, 11, 8)), entity.endTime)
    }

    @Test
    fun settingsSurviveARoundTrip() {
        val settings = TimelineSettings(
            zoom = ZoomLevel(TimeUnit.WEEK, 90f),
            widthMultiplier = 1.5f,
            snapIntervals = mapOf(
                TimeUnit.HOUR to SnapInterval(30),
                TimeUnit.DAY to SnapInterval(120),
                TimeUnit.WEEK to SnapInterval(SnapInterval.DAY_MINUTES),
                TimeUnit.MONTH to SnapInterval(SnapInterval.WEEK_MINUTES),
            ),
            rowHeightDp = 80f,
            leftPaneExpanded = true,
        )
        assertEquals(settings, settings.toEntity().toDomain())
    }

    @Test
    fun withScheduleChangesOnlyTheTimeColumns() {
        val before = task(TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30"))))
            .toEntity()
        val after = before.withSchedule(releaseDay)
        assertEquals(
            before.copy(startTime = after.startTime, endTime = after.endTime, isAllDay = true),
            after,
        )
    }

    @Test
    fun singleDayIsExactlyOneUtcDay() {
        val entity = task(releaseDay).toEntity()
        assertEquals(24 * 60 * 60 * 1_000L, entity.endTime - entity.startTime)
    }

    @Test
    fun missingSnapFallsBackToTheDefault() {
        val settings = TimelineSettings.DEFAULT.copy(snapIntervals = mapOf(TimeUnit.HOUR to SnapInterval(30)))
        val entity = settings.toEntity()
        assertEquals(30, entity.snapHourMinutes)
        assertEquals(SnapInterval.default(TimeUnit.MONTH).minutes, entity.snapMonthMinutes)
    }

    @Test
    fun allDayDatesAreTheSameInEveryTimeZone() {
        val entity = task(releaseDay).toEntity()
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("Asia/Taipei", "America/New_York", "Pacific/Kiritimati")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(zone, releaseDay, entity.toDomain().schedule)
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun allDayBefore1970KeepsItsDate() {
        val newYearsEve = TaskSchedule.AllDay(DateRange(LocalDate.of(1969, 12, 31), LocalDate.of(1970, 1, 1)))
        val entity = task(newYearsEve).toEntity()
        assertTrue(entity.startTime < 0)
        assertEquals(newYearsEve, entity.toDomain().schedule)
    }

    @Test
    fun allDayStoredOffMidnightStaysOnItsDate() {
        val threeHours = 3 * 60 * 60 * 1_000L
        val entity = task(releaseDay).toEntity().let { it.copy(startTime = it.startTime + threeHours) }
        assertEquals(releaseDay, entity.toDomain().schedule)
    }
}
