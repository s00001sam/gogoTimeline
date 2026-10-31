package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.core.text.TaskTitle
import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.local.BoardEntity
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
    override fun observeBoards(): Flow<List<Board>> = boardDataSource.observeBoards().map { rows ->
        rows.map { it.toDomain() }
    }

    override fun observeBoard(id: Long): Flow<Board?> = boardDataSource.observeBoard(id).map { it?.toDomain() }

    override suspend fun getStartBoardId(): Long? = boardDataSource.getMostRecentBoardId()

    override suspend fun saveAnchor(
        boardId: Long,
        taskId: Long?,
    ) = boardDataSource.updateAnchor(boardId, taskId)

    override suspend fun markOpened(boardId: Long) = boardDataSource.updateLastOpenedAt(boardId, clock.millis())

    override suspend fun create(name: String): Long {
        val trimmed = name.trim()
        require(TaskTitle.isValid(trimmed)) { "Invalid board name" }
        return boardDataSource.insert(
            BoardEntity(name = trimmed, anchorTaskId = null, lastOpenedAt = clock.millis()),
        )
    }

    override suspend fun rename(
        boardId: Long,
        name: String,
    ) {
        val trimmed = name.trim()
        require(TaskTitle.isValid(trimmed)) { "Invalid board name" }
        boardDataSource.updateName(boardId, trimmed)
    }

    override suspend fun delete(boardId: Long) = boardDataSource.delete(boardId)
}
