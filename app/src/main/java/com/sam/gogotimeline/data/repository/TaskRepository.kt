package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(boardId: Long): Flow<List<Task>>

    /** Adds the task at row [atIndex] (the last row when null), shifting the rows below; returns its id. */
    suspend fun createTask(
        boardId: Long,
        title: String,
        schedule: TaskSchedule,
        colorIndex: Int,
        notes: String,
        atIndex: Int? = null,
    ): Long

    /**
     * Writes only when something changed, and moves the task to row [toIndex] when given, in one transaction so a
     * failure keeps neither; throws if the task no longer exists.
     */
    suspend fun saveTask(
        edit: TaskEdit,
        toIndex: Int? = null,
    )

    suspend fun moveTask(
        boardId: Long,
        taskId: Long,
        toIndex: Int,
    )

    suspend fun deleteTask(
        boardId: Long,
        taskId: Long,
    )
}
