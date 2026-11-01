package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.NEW_YORK
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import com.sam.gogotimeline.data.model.TaskSortMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardDisplayTest {
    private val now = TAIPEI.at("2026-10-31T10:00")

    private fun task(
        id: Long,
        schedule: TaskSchedule,
    ) = Task(
        id = id,
        boardId = 1,
        title = "Task $id",
        schedule = schedule,
        colorIndex = 0,
        sortOrder = id.toInt(),
        createdAt = TAIPEI.at("2026-10-16T09:00"),
        updatedAt = TAIPEI.at("2026-10-20T09:00"),
    )

    private val kickoff = TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30"))
    private val release = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)))
    private val rows = (0L..3L).map { task(it, TaskSchedule.Timed(kickoff)).toTimelineTask(TAIPEI) }

    @Test
    fun timedTaskKeepsItsInstants() {
        val shown = task(1, TaskSchedule.Timed(kickoff)).toTimelineTask(TAIPEI)
        assertEquals(kickoff.start, shown.start)
        assertEquals(kickoff.end, shown.end)
        assertFalse(shown.isAllDay)
    }

    @Test
    fun opensOnTheAnchorAtTheLeftMargin() {
        assertEquals(
            OpeningPosition(rowIndex = 2, time = kickoff.start, centered = false),
            openingPosition(rows, 2, now),
        )
    }

    @Test
    fun noAnchorOpensOnTheFirstTask() {
        assertEquals(0, openingPosition(rows, anchorTaskId = null, now = now).rowIndex)
    }

    @Test
    fun deletedAnchorOpensOnTheFirstTask() {
        assertEquals(0, openingPosition(rows, anchorTaskId = 99, now = now).rowIndex)
    }

    @Test
    fun allDayStartsAtLocalMidnightInEachZone() {
        val inTaipei = task(1, release).toTimelineTask(TAIPEI)
        val inNewYork = task(1, release).toTimelineTask(NEW_YORK)
        assertEquals(TAIPEI.at("2026-11-07T00:00"), inTaipei.start)
        assertEquals(NEW_YORK.at("2026-11-07T00:00"), inNewYork.start)
        assertTrue(inTaipei.isAllDay && inNewYork.isAllDay)
    }

    @Test
    fun noTasksOpenOnNowCentered() {
        assertEquals(OpeningPosition(rowIndex = 0, time = now, centered = true), openingPosition(emptyList(), 1, now))
    }

    private val domainTasks = (1L..3L).map { task(it, TaskSchedule.Timed(kickoff)) }
    private val later = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T14:30")))

    @Test
    fun draftReplacesOnlyItsTask() {
        val shown = tasksWithDraft(domainTasks, TaskEdit(2, "Renamed", later, colorIndex = 5))
        assertEquals(listOf(1L, 2L, 3L), shown.map { it.id })
        assertEquals(
            TaskEdit(2, "Renamed", later, 5),
            shown[1].let {
                TaskEdit(it.id, it.title, it.schedule, it.colorIndex)
            },
        )
        assertEquals(domainTasks[0], shown[0])
        assertEquals(domainTasks[2], shown[2])
    }

    @Test
    fun noDraftKeepsTheTasks() {
        assertEquals(domainTasks, tasksWithDraft(domainTasks, draft = null))
    }

    @Test
    fun draftOfAMissingTaskKeepsTheTasks() {
        assertEquals(domainTasks, tasksWithDraft(domainTasks, TaskEdit(99, "Gone", later, 0)))
    }

    @Test
    fun newTaskDraftIsTheLastRow() {
        val shown = tasksWithDraft(domainTasks, TaskEdit(TaskEdit.NEW_TASK_ID, "", later, colorIndex = 2))
        assertEquals(listOf(1L, 2L, 3L, TaskEdit.NEW_TASK_ID), shown.map { it.id })
        assertEquals(3, shown.last().sortOrder)
        assertEquals(later, shown.last().schedule)
        assertEquals(domainTasks, shown.dropLast(1))
    }

    @Test
    fun newTaskOnAnEmptyBoardIsTheOnlyRow() {
        val shown = tasksWithDraft(emptyList(), TaskEdit(TaskEdit.NEW_TASK_ID, "", later, colorIndex = 2))
        assertEquals(listOf(TaskEdit.NEW_TASK_ID), shown.map { it.id })
        assertEquals(0, shown.single().sortOrder)
    }

    @Test
    fun draftRowMovesTheEditedTask() {
        val shown = tasksWithDraft(domainTasks, TaskEdit(1, "Task 1", later, colorIndex = 0), draftRow = 2)
        assertEquals(listOf(2L, 3L, 1L), shown.map { it.id })
    }

    @Test
    fun newTaskDraftCanSitAtTheTop() {
        val shown = tasksWithDraft(domainTasks, TaskEdit(TaskEdit.NEW_TASK_ID, "", later, 2), draftRow = 0)
        assertEquals(listOf(TaskEdit.NEW_TASK_ID, 1L, 2L, 3L), shown.map { it.id })
    }

    @Test
    fun draftRowPastTheEndStaysLast() {
        val shown = tasksWithDraft(domainTasks, TaskEdit(1, "Task 1", later, colorIndex = 0), draftRow = 99)
        assertEquals(listOf(2L, 3L, 1L), shown.map { it.id })
    }

    private fun timed(
        id: Long,
        order: Int,
        start: String,
        end: String,
    ) = task(id, TaskSchedule.Timed(TimeRange(TAIPEI.at(start), TAIPEI.at(end)))).copy(sortOrder = order)

    // Manual order 1, 2, 3, 4: a long late task, a short early one, a mid one, and an all-day one on Nov 7.
    private val mixed = listOf(
        timed(1, 0, "2026-10-31T13:00", "2026-10-31T18:00"),
        timed(2, 1, "2026-10-31T08:00", "2026-10-31T08:30"),
        timed(3, 2, "2026-10-31T10:00", "2026-10-31T12:00"),
        task(4, release).copy(sortOrder = 3),
    )

    @Test
    fun sortsByStartTimeAndByDurationAndManualFollowsSortOrder() {
        assertEquals(listOf(2L, 3L, 1L, 4L), mixed.sortedFor(TaskSortMode.START_TIME, TAIPEI).map { it.id })
        assertEquals(listOf(2L, 3L, 1L, 4L), mixed.sortedFor(TaskSortMode.DURATION, TAIPEI).map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), mixed.reversed().sortedFor(TaskSortMode.MANUAL, TAIPEI).map { it.id })
    }

    @Test
    fun tiesKeepTheManualOrder() {
        val same = listOf(
            timed(5, 2, "2026-10-31T09:00", "2026-10-31T10:00"),
            timed(6, 0, "2026-10-31T09:00", "2026-10-31T10:00"),
            timed(7, 1, "2026-10-31T09:00", "2026-10-31T10:00"),
        )
        assertEquals(listOf(6L, 7L, 5L), same.sortedFor(TaskSortMode.START_TIME, TAIPEI).map { it.id })
        assertEquals(listOf(6L, 7L, 5L), same.sortedFor(TaskSortMode.DURATION, TAIPEI).map { it.id })
    }

    @Test
    fun allDayTasksStartAtLocalMidnightInTheCurrentZone() {
        // A Taipei 09:00 task on Nov 7 is Nov 6 20:00 in New York, before New York's Nov 7 00:00.
        val morning = timed(8, 0, "2026-11-07T09:00", "2026-11-07T10:00")
        val allDay = task(9, release).copy(sortOrder = 1)
        assertEquals(listOf(9L, 8L), listOf(morning, allDay).sortedFor(TaskSortMode.START_TIME, TAIPEI).map { it.id })
        assertEquals(listOf(8L, 9L), listOf(morning, allDay).sortedFor(TaskSortMode.START_TIME, NEW_YORK).map { it.id })
        assertTrue(emptyList<Task>().sortedFor(TaskSortMode.DURATION, TAIPEI).isEmpty())
    }
}
