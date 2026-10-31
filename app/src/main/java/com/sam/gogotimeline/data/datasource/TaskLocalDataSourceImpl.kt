package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.data.local.TaskDao
import com.sam.gogotimeline.data.local.TaskEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TaskLocalDataSourceImpl @Inject constructor(private val taskDao: TaskDao) : TaskLocalDataSource {
    override fun observeTasks(boardId: Long): Flow<List<TaskEntity>> = taskDao.observeTasks(boardId)

    override suspend fun getTask(id: Long): TaskEntity? = taskDao.getTask(id)

    override suspend fun maxSortOrder(boardId: Long): Int? = taskDao.maxSortOrder(boardId)

    override suspend fun insert(task: TaskEntity): Long = taskDao.insert(task)

    override suspend fun update(task: TaskEntity) = taskDao.update(task)

    override suspend fun shiftSortOrders(
        boardId: Long,
        from: Int,
        to: Int,
        delta: Int,
    ) = taskDao.shiftSortOrders(boardId, from, to, delta)

    override suspend fun setSortOrder(
        id: Long,
        order: Int,
    ) = taskDao.setSortOrder(id, order)

    override suspend fun delete(id: Long) = taskDao.delete(id)
}
