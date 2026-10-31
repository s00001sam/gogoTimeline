package com.sam.gogotimeline.data.local

import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.data.model.Board
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TimelineSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    boardId = boardId,
    title = title,
    schedule = if (isAllDay) {
        TaskSchedule.AllDay(DateRange(startTime.toUtcDate(), endTime.toUtcDate()))
    } else {
        TaskSchedule.Timed(TimeRange(startTime, endTime))
    },
    colorIndex = colorIndex,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    notes = notes,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    boardId = boardId,
    title = title,
    startTime = 0,
    endTime = 0,
    colorIndex = colorIndex,
    isAllDay = false,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    notes = notes,
).withSchedule(schedule)

/** Replaces only the three time columns; all-day dates are stored as UTC 00:00 so every zone reads the same dates. */
fun TaskEntity.withSchedule(schedule: TaskSchedule): TaskEntity = when (schedule) {
    is TaskSchedule.Timed -> copy(
        startTime = schedule.range.start,
        endTime = schedule.range.end,
        isAllDay = false,
    )

    is TaskSchedule.AllDay -> copy(
        startTime = schedule.dates.start.toUtcMillis(),
        endTime = schedule.dates.end.toUtcMillis(),
        isAllDay = true,
    )
}

fun BoardEntity.toDomain(): Board = Board(
    id = id,
    name = name,
    anchorTaskId = anchorTaskId,
    lastOpenedAt = lastOpenedAt,
)

fun SettingsEntity.toDomain(): TimelineSettings = TimelineSettings(
    zoom = ZoomLevel(unit, dpPerUnit),
    widthMultiplier = widthMultiplier,
    snapIntervals = mapOf(
        TimeUnit.HOUR to SnapInterval(snapHourMinutes),
        TimeUnit.DAY to SnapInterval(snapDayMinutes),
        TimeUnit.WEEK to SnapInterval(snapWeekMinutes),
        TimeUnit.MONTH to SnapInterval(snapMonthMinutes),
    ),
    rowHeightDp = rowHeightDp,
    leftPaneExpanded = leftPaneExpanded,
)

fun TimelineSettings.toEntity(): SettingsEntity = SettingsEntity(
    unit = zoom.unit,
    dpPerUnit = zoom.dpPerUnit,
    widthMultiplier = widthMultiplier,
    snapHourMinutes = snapFor(TimeUnit.HOUR).minutes,
    snapDayMinutes = snapFor(TimeUnit.DAY).minutes,
    snapWeekMinutes = snapFor(TimeUnit.WEEK).minutes,
    snapMonthMinutes = snapFor(TimeUnit.MONTH).minutes,
    rowHeightDp = rowHeightDp,
    leftPaneExpanded = leftPaneExpanded,
)

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
