package com.sam.gogotimeline.data.model

/**
 * A board. [anchorTaskId] is the task shown at the top when the board opens (spec 6.7). A [locked] board allows no task
 * changes (spec 9).
 */
data class Board(
    val id: Long,
    val name: String,
    val anchorTaskId: Long?,
    val lastOpenedAt: Long,
    val locked: Boolean = false,
)
