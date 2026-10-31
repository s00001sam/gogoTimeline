package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.data.model.Board
import kotlinx.coroutines.flow.Flow

interface BoardRepository {
    fun observeBoard(id: Long): Flow<Board?>

    /** The most recently opened board, or null when there is none. */
    suspend fun getStartBoardId(): Long?

    suspend fun saveAnchor(
        boardId: Long,
        taskId: Long?,
    )

    suspend fun markOpened(boardId: Long)
}
