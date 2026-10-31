package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

interface TaskLocalDataSource {
    fun observeTasks(boardId: Long): Flow<List<TaskEntity>>

    suspend fun getTask(id: Long): TaskEntity?

    suspend fun maxSortOrder(boardId: Long): Int?

    suspend fun insert(task: TaskEntity): Long

    suspend fun update(task: TaskEntity)

    suspend fun shiftSortOrders(
        boardId: Long,
        from: Int,
        to: Int,
        delta: Int,
    )

    suspend fun setSortOrder(
        id: Long,
        order: Int,
    )

    suspend fun delete(id: Long)
}
