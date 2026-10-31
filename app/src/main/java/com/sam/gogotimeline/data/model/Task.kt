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

/**
 * What to save for a task; [NEW_TASK_ID] marks one not in the database yet. Order and timestamps are decided by the
 * repository.
 */
data class TaskEdit(
    val taskId: Long,
    val title: String,
    val schedule: TaskSchedule,
    val colorIndex: Int,
    val notes: String = "",
) {
    val isNew: Boolean get() = taskId == NEW_TASK_ID

    companion object {
        // Room ids start at 1, so 0 never belongs to a saved task.
        const val NEW_TASK_ID = 0L
    }
}
