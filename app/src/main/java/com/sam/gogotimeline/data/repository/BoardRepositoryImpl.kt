package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.local.toDomain
import com.sam.gogotimeline.data.model.Board
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BoardRepositoryImpl @Inject constructor(
    private val boardDataSource: BoardLocalDataSource,
    private val clock: Clock,
) : BoardRepository {
    override fun observeBoard(id: Long): Flow<Board?> = boardDataSource.observeBoard(id).map { it?.toDomain() }

    override suspend fun getStartBoardId(): Long? = boardDataSource.getMostRecentBoardId()

    override suspend fun saveAnchor(
        boardId: Long,
        taskId: Long?,
    ) = boardDataSource.updateAnchor(boardId, taskId)

    override suspend fun markOpened(boardId: Long) = boardDataSource.updateLastOpenedAt(boardId, clock.millis())
}
