package com.sam.gogotimeline.data.model

import com.sam.gogotimeline.core.time.TaskSchedule

/** A task as the app uses it. Its time is a [TaskSchedule], so "all-day at 09:00" cannot exist. */
data class Task(
    val id: Long,
    val boardId: Long,
    val title: String,
    val schedule: TaskSchedule,
    val colorIndex: Int,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val notes: String = "",
)

/** What to save for an existing task. Order and timestamps are decided by the repository. */
data class TaskEdit(
    val taskId: Long,
    val title: String,
    val schedule: TaskSchedule,
    val colorIndex: Int,
    val notes: String = "",
)
