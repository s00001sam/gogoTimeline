package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.data.local.BoardEntity
import kotlinx.coroutines.flow.Flow

interface BoardLocalDataSource {
    fun observeBoards(): Flow<List<BoardEntity>>

    fun observeBoard(id: Long): Flow<BoardEntity?>

    suspend fun getBoard(id: Long): BoardEntity?

    suspend fun getMostRecentBoardId(): Long?

    suspend fun updateAnchor(
        id: Long,
        taskId: Long?,
    )

    suspend fun updateLastOpenedAt(
        id: Long,
        time: Long,
    )

    suspend fun clearAnchor(taskId: Long)

    suspend fun insert(board: BoardEntity): Long

    suspend fun updateName(
        id: Long,
        name: String,
    )

    suspend fun delete(id: Long)

    suspend fun updateLocked(
        id: Long,
        locked: Boolean,
    )
}
