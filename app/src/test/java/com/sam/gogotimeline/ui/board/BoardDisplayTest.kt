package com.sam.gogotimeline.ui.board

import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.NEW_YORK
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
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
}
