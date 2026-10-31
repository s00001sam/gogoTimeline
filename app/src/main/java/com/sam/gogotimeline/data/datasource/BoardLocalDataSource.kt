package com.sam.gogotimeline.data.datasource

import com.sam.gogotimeline.data.local.BoardEntity
import kotlinx.coroutines.flow.Flow

interface BoardLocalDataSource {
    fun observeBoard(id: Long): Flow<BoardEntity?>

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
}
