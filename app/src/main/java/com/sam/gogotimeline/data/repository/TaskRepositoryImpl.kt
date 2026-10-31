package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.datasource.TaskLocalDataSource
import com.sam.gogotimeline.data.datasource.TransactionRunner
import com.sam.gogotimeline.data.local.TaskEntity
import com.sam.gogotimeline.data.local.toDomain
import com.sam.gogotimeline.data.local.withSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
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
    ): Long = transaction.run {
        val now = clock.millis()
        val task = TaskEntity(
            boardId = boardId,
            title = title,
            startTime = 0,
            endTime = 0,
            colorIndex = colorIndex,
            isAllDay = false,
            sortOrder = (taskDataSource.maxSortOrder(boardId) ?: -1) + 1,
            createdAt = now,
            updatedAt = now,
        ).withSchedule(schedule)
        taskDataSource.insert(task)
    }

    override suspend fun saveTask(edit: TaskEdit) {
        transaction.run {
            val current = checkNotNull(taskDataSource.getTask(edit.taskId)) { "Task ${edit.taskId} no longer exists" }
            val edited = current.copy(title = edit.title, colorIndex = edit.colorIndex).withSchedule(edit.schedule)
            if (edited != current) taskDataSource.update(edited.copy(updatedAt = clock.millis()))
        }
    }

    override suspend fun moveTask(
        boardId: Long,
        taskId: Long,
        toIndex: Int,
    ) {
        transaction.run {
            val from = taskDataSource.getTask(taskId)?.sortOrder ?: return@run
            val to = toIndex.coerceIn(0, taskDataSource.maxSortOrder(boardId) ?: 0)
            when {
                to > from -> taskDataSource.shiftSortOrders(boardId, from + 1, to, -1)
                to < from -> taskDataSource.shiftSortOrders(boardId, to, from - 1, 1)
                else -> return@run
            }
            taskDataSource.setSortOrder(taskId, to)
        }
    }

    override suspend fun deleteTask(
        boardId: Long,
        taskId: Long,
    ) {
        transaction.run {
            val order = taskDataSource.getTask(taskId)?.sortOrder ?: return@run
            taskDataSource.delete(taskId)
            taskDataSource.shiftSortOrders(boardId, order + 1, Int.MAX_VALUE, -1)
            boardDataSource.clearAnchor(taskId)
        }
    }
}
