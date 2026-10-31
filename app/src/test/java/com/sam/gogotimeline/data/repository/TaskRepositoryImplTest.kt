package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.datasource.TaskLocalDataSource
import com.sam.gogotimeline.data.local.TaskEntity
import com.sam.gogotimeline.data.model.TaskEdit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryImplTest {
    private val now = TAIPEI.at("2026-10-31T10:00")
    private val tasks = mockk<TaskLocalDataSource>(relaxed = true)
    private val boards = mockk<BoardLocalDataSource>(relaxed = true)
    private val transaction = RecordingTransactionRunner()
    private val repository = TaskRepositoryImpl(
        taskDataSource = tasks,
        boardDataSource = boards,
        transaction = transaction,
        clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC),
    )

    private val kickoff = TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30"))

    private fun entity(order: Int) = TaskEntity(
        id = TASK,
        boardId = BOARD,
        title = "Kickoff meeting",
        startTime = kickoff.start,
        endTime = kickoff.end,
        colorIndex = 0,
        isAllDay = false,
        sortOrder = order,
        createdAt = TAIPEI.at("2026-10-16T09:00"),
        updatedAt = TAIPEI.at("2026-10-20T09:00"),
    )

    private fun givenTask(order: Int?) {
        coEvery { tasks.getTask(TASK) } returns order?.let { entity(it) }
        coEvery { tasks.maxSortOrder(BOARD) } returns 14
    }

    private fun edit(range: TimeRange) = TaskEdit(TASK, "Kickoff meeting", TaskSchedule.Timed(range), colorIndex = 0)

    @Test
    fun moveDownShiftsTheRowsInBetweenUp() = runTest {
        givenTask(order = 0)
        repository.moveTask(BOARD, TASK, toIndex = 3)
        coVerifyOrder {
            tasks.shiftSortOrders(BOARD, 1, 3, -1)
            tasks.setSortOrder(TASK, 3)
        }
        assertEquals(1, transaction.runs)
    }

    @Test
    fun deleteClosesTheGapAndClearsTheAnchorInOneTransaction() = runTest {
        givenTask(order = 5)
        val log = mutableListOf<String>()
        coEvery { tasks.delete(TASK) } answers { log += "delete ${transaction.active}" }
        coEvery { tasks.shiftSortOrders(BOARD, 6, Int.MAX_VALUE, -1) } answers { log += "shift ${transaction.active}" }
        coEvery { boards.clearAnchor(TASK) } answers { log += "clearAnchor ${transaction.active}" }

        repository.deleteTask(BOARD, TASK)

        assertEquals(listOf("delete true", "shift true", "clearAnchor true"), log)
        assertEquals(1, transaction.runs)
    }

    @Test
    fun savingANewTimeUpdatesTheEditTime() = runTest {
        givenTask(order = 0)
        val saved = slot<TaskEntity>()
        coEvery { tasks.update(capture(saved)) } answers {}
        val later = TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T14:30"))

        repository.saveTask(edit(later))

        assertEquals(entity(0).copy(startTime = later.start, endTime = later.end, updatedAt = now), saved.captured)
    }

    @Test
    fun newTaskGoesToTheLastRow() = runTest {
        coEvery { tasks.maxSortOrder(BOARD) } returns 14
        val inserted = slot<TaskEntity>()
        coEvery { tasks.insert(capture(inserted)) } returns 16

        val id = repository.createTask(BOARD, "Demo", TaskSchedule.Timed(kickoff), colorIndex = 3)

        assertEquals(16, id)
        assertEquals(15, inserted.captured.sortOrder)
        assertEquals(now, inserted.captured.createdAt)
        assertEquals(now, inserted.captured.updatedAt)
    }

    @Test
    fun observedEntitiesBecomeTasks() = runTest {
        coEvery { tasks.observeTasks(BOARD) } returns flowOf(listOf(entity(0)))
        val task = repository.observeTasks(BOARD).first().single()
        assertEquals(TaskSchedule.Timed(kickoff), task.schedule)
        assertEquals(entity(0).createdAt, task.createdAt)
        assertEquals(entity(0).updatedAt, task.updatedAt)
    }

    @Test
    fun moveUpShiftsTheRowsInBetweenDown() = runTest {
        givenTask(order = 3)
        repository.moveTask(BOARD, TASK, toIndex = 0)
        coVerifyOrder {
            tasks.shiftSortOrders(BOARD, 0, 2, 1)
            tasks.setSortOrder(TASK, 0)
        }
    }

    @Test
    fun moveToTheSameRowDoesNothing() = runTest {
        givenTask(order = 4)
        repository.moveTask(BOARD, TASK, toIndex = 4)
        coVerify(exactly = 0) { tasks.shiftSortOrders(any(), any(), any(), any()) }
        coVerify(exactly = 0) { tasks.setSortOrder(any(), any()) }
    }

    @Test
    fun moveTargetIsClampedToTheRows() = runTest {
        givenTask(order = 5)
        repository.moveTask(BOARD, TASK, toIndex = 99)
        repository.moveTask(BOARD, TASK, toIndex = -1)
        coVerifyOrder {
            tasks.setSortOrder(TASK, 14)
            tasks.setSortOrder(TASK, 0)
        }
    }

    @Test
    fun savingWithoutChangesWritesNothing() = runTest {
        givenTask(order = 0)
        repository.saveTask(edit(kickoff))
        coVerify(exactly = 0) { tasks.update(any()) }
    }

    @Test
    fun firstTaskOfAnEmptyBoardIsRowZero() = runTest {
        coEvery { tasks.maxSortOrder(BOARD) } returns null
        val inserted = slot<TaskEntity>()
        coEvery { tasks.insert(capture(inserted)) } returns 1
        repository.createTask(BOARD, "First", TaskSchedule.Timed(kickoff), colorIndex = 0)
        assertEquals(0, inserted.captured.sortOrder)
    }

    @Test
    fun savingADeletedTaskFails() = runTest {
        givenTask(order = null)
        val result = runCatching { repository.saveTask(edit(kickoff)) }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { tasks.update(any()) }
    }

    @Test
    fun movingOrDeletingAMissingTaskChangesNothing() = runTest {
        givenTask(order = null)
        repository.moveTask(BOARD, TASK, toIndex = 3)
        repository.deleteTask(BOARD, TASK)
        coVerify(exactly = 0) { tasks.shiftSortOrders(any(), any(), any(), any()) }
        coVerify(exactly = 0) { tasks.setSortOrder(any(), any()) }
        coVerify(exactly = 0) { tasks.delete(any()) }
        coVerify(exactly = 0) { boards.clearAnchor(any()) }
    }

    @Test
    fun failureMidDeleteStopsAndPropagates() = runTest {
        givenTask(order = 5)
        coEvery { tasks.shiftSortOrders(any(), any(), any(), any()) } throws IllegalStateException("disk full")
        val result = runCatching { repository.deleteTask(BOARD, TASK) }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { boards.clearAnchor(any()) }
    }

    @Test
    fun savedNameIsTrimmed() = runTest {
        givenTask(order = 0)
        val saved = slot<TaskEntity>()
        coEvery { tasks.update(capture(saved)) } answers {}
        repository.saveTask(TaskEdit(TASK, "  Kickoff  ", TaskSchedule.Timed(kickoff), colorIndex = 1))
        assertEquals("Kickoff", saved.captured.title)
    }

    @Test
    fun nameOfFortyOneCharactersIsRejected() = runTest {
        givenTask(order = 0)
        val result = runCatching { repository.saveTask(TaskEdit(TASK, "a".repeat(41), TaskSchedule.Timed(kickoff), 0)) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { tasks.update(any()) }
    }

    @Test
    fun blankNameIsRejected() = runTest {
        givenTask(order = 0)
        val result = runCatching { repository.saveTask(TaskEdit(TASK, "   ", TaskSchedule.Timed(kickoff), 0)) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { tasks.update(any()) }
        val created = runCatching { repository.createTask(BOARD, "", TaskSchedule.Timed(kickoff), 0) }
        assertTrue(created.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { tasks.insert(any()) }
    }

    private companion object {
        const val BOARD = 1L
        const val TASK = 10L
    }
}
