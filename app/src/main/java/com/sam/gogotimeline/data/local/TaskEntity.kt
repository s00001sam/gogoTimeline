package com.sam.gogotimeline.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * When [isAllDay] is true, [startTime] and [endTime] hold dates encoded as UTC 00:00 (end exclusive), so the
 * task stays on the same dates in every time zone; otherwise they are instants.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("boardId", "sortOrder")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boardId: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val colorIndex: Int,
    val isAllDay: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val notes: String = "",
)
