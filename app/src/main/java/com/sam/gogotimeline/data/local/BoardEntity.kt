package com.sam.gogotimeline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * `anchorTaskId` has no foreign key: tasks already point at boards, and pointing back would form a cycle.
 * The repository clears it in the same transaction that deletes the task.
 */
@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val anchorTaskId: Long?,
    val lastOpenedAt: Long,
)
