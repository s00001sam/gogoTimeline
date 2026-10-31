package com.sam.gogotimeline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE boardId = :boardId ORDER BY sortOrder")
    fun observeTasks(boardId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE boardId = :boardId")
    suspend fun maxSortOrder(boardId: Long): Int?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        "UPDATE tasks SET sortOrder = sortOrder + :delta " +
            "WHERE boardId = :boardId AND sortOrder BETWEEN :from AND :to",
    )
    suspend fun shiftSortOrders(
        boardId: Long,
        from: Int,
        to: Int,
        delta: Int,
    )

    @Query("UPDATE tasks SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(
        id: Long,
        order: Int,
    )

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
