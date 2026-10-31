package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.data.model.Task
import com.sam.gogotimeline.data.model.TaskEdit
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(boardId: Long): Flow<List<Task>>

    /** Adds the task as the last row and returns its id. */
    suspend fun createTask(
        boardId: Long,
        title: String,
        schedule: TaskSchedule,
        colorIndex: Int,
    ): Long

    /** Writes only when something changed; throws if the task no longer exists. */
    suspend fun saveTask(edit: TaskEdit)

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
