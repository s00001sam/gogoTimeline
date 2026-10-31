package com.sam.gogotimeline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards WHERE id = :id")
    fun observeBoard(id: Long): Flow<BoardEntity?>

    @Query("SELECT id FROM boards ORDER BY lastOpenedAt DESC LIMIT 1")
    suspend fun getMostRecentBoardId(): Long?

    @Query("UPDATE boards SET anchorTaskId = :taskId WHERE id = :id")
    suspend fun updateAnchor(
        id: Long,
        taskId: Long?,
    )

    @Query("UPDATE boards SET lastOpenedAt = :time WHERE id = :id")
    suspend fun updateLastOpenedAt(
        id: Long,
        time: Long,
    )

    @Query("UPDATE boards SET anchorTaskId = NULL WHERE anchorTaskId = :taskId")
    suspend fun clearAnchor(taskId: Long)

    @Insert
    suspend fun insert(board: BoardEntity): Long

    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun delete(id: Long)
}
