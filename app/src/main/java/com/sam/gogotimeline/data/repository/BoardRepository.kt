package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.data.model.Board
import kotlinx.coroutines.flow.Flow

interface BoardRepository {
    /** All boards, most recently opened first. */
    fun observeBoards(): Flow<List<Board>>

    fun observeBoard(id: Long): Flow<Board?>

    /** The most recently opened board, or null when there is none. */
    suspend fun getStartBoardId(): Long?

    suspend fun saveAnchor(
        boardId: Long,
        taskId: Long?,
    )

    suspend fun markOpened(boardId: Long)

    suspend fun create(name: String): Long

    suspend fun rename(
        boardId: Long,
        name: String,
    )

    suspend fun delete(boardId: Long)

    /** A locked board blocks task changes only; it still scrolls, zooms, and can be renamed or deleted. */
    suspend fun setLocked(
        boardId: Long,
        locked: Boolean,
    )
}
