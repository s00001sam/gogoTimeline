package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.text.TaskNotes
import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.datasource.TaskLocalDataSource
import com.sam.gogotimeline.data.datasource.TransactionRunner
import com.sam.gogotimeline.data.local.TaskEntity
import com.sam.gogotimeline.data.local.toDomain
import com.sam.gogotimeline.data.local.withSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import com.sam.gogotimeline.data.model.TaskValidator
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDataSource: TaskLocalDataSource,
    private val boardDataSource: BoardLocalDataSource,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : TaskRepository {
    override fun observeTasks(boardId: Long): Flow<List<Task>> =
        taskDataSource.observeTasks(boardId).map { tasks -> tasks.map { it.toDomain() } }

    override suspend fun createTask(
        boardId: Long,
        title: String,
        schedule: TaskSchedule,
        colorIndex: Int,
        notes: String,
        atIndex: Int?,
    ): Long = transaction.run {
        require(TaskTitle.isValid(title)) { "Invalid task name" }
        require(TaskNotes.isValid(notes)) { "Notes too long" }
        TaskValidator.requireValid(schedule, colorIndex)
        requireEditableBoard(boardId)
        val now = clock.millis()
        // Rows from the new one's place down move one row first, so no two rows ever share a sortOrder.
        val count = (taskDataSource.maxSortOrder(boardId) ?: -1) + 1
        val sortOrder = atIndex?.coerceIn(0, count) ?: count
        if (sortOrder < count) taskDataSource.shiftSortOrders(boardId, sortOrder, count - 1, 1)
        val task = TaskEntity(
            boardId = boardId,
            title = title.trim(),
            notes = TaskNotes.normalize(notes),
            startTime = 0,
            endTime = 0,
            colorIndex = colorIndex,
            isAllDay = false,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
        ).withSchedule(schedule)
        taskDataSource.insert(task)
    }

    override suspend fun saveTask(
        edit: TaskEdit,
        toIndex: Int?,
    ) {
        transaction.run {
            // The sheet checks the name too; checking here keeps bad names out whoever saves.
            require(TaskTitle.isValid(edit.title)) { "Invalid task name" }
            require(TaskNotes.isValid(edit.notes)) { "Notes too long" }
            TaskValidator.requireValid(edit.schedule, edit.colorIndex)
            val current = checkNotNull(taskDataSource.getTask(edit.taskId)) { "Task ${edit.taskId} no longer exists" }
            requireEditableBoard(current.boardId)
            val edited = current.copy(
                title = edit.title.trim(),
                notes = TaskNotes.normalize(edit.notes),
                colorIndex = edit.colorIndex,
            ).withSchedule(edit.schedule)
            if (edited != current) taskDataSource.update(edited.copy(updatedAt = clock.millis()))
            if (toIndex != null) moveInTransaction(current.boardId, edit.taskId, current.sortOrder, toIndex)
        }
    }

    override suspend fun moveTask(
        boardId: Long,
        taskId: Long,
        toIndex: Int,
    ) {
        transaction.run {
            val task = taskDataSource.getTask(taskId) ?: return@run
            check(task.boardId == boardId) { "Task $taskId does not belong to board $boardId" }
            requireEditableBoard(boardId)
            moveInTransaction(boardId, taskId, task.sortOrder, toIndex)
        }
    }

    // Shifts the rows between the old and new place by one and puts the task there; callers hold the transaction.
    private suspend fun moveInTransaction(
        boardId: Long,
        taskId: Long,
        from: Int,
        toIndex: Int,
    ) {
        val to = toIndex.coerceIn(0, taskDataSource.maxSortOrder(boardId) ?: 0)
        when {
            to > from -> taskDataSource.shiftSortOrders(boardId, from + 1, to, -1)
            to < from -> taskDataSource.shiftSortOrders(boardId, to, from - 1, 1)
            else -> return
        }
        taskDataSource.setSortOrder(taskId, to)
    }

    override suspend fun deleteTask(
        boardId: Long,
        taskId: Long,
    ) {
        transaction.run {
            val task = taskDataSource.getTask(taskId) ?: return@run
            check(task.boardId == boardId) { "Task $taskId does not belong to board $boardId" }
            requireEditableBoard(boardId)
            taskDataSource.delete(taskId)
            taskDataSource.shiftSortOrders(boardId, task.sortOrder + 1, Int.MAX_VALUE, -1)
            boardDataSource.clearAnchor(taskId)
        }
    }

    private suspend fun requireEditableBoard(boardId: Long) {
        val board = checkNotNull(boardDataSource.getBoard(boardId)) { "Board $boardId no longer exists" }
        check(!board.locked) { "Board $boardId is locked" }
    }
}
