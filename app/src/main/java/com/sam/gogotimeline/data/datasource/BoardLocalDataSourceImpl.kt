package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.data.local.BoardDao
import com.sam.gogotimeline.data.local.BoardEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BoardLocalDataSourceImpl @Inject constructor(private val boardDao: BoardDao) : BoardLocalDataSource {
    override fun observeBoard(id: Long): Flow<BoardEntity?> = boardDao.observeBoard(id)

    override suspend fun getMostRecentBoardId(): Long? = boardDao.getMostRecentBoardId()

    override suspend fun updateAnchor(
        id: Long,
        taskId: Long?,
    ) = boardDao.updateAnchor(id, taskId)

    override suspend fun updateLastOpenedAt(
        id: Long,
        time: Long,
    ) = boardDao.updateLastOpenedAt(id, time)

    override suspend fun clearAnchor(taskId: Long) = boardDao.clearAnchor(taskId)
}
