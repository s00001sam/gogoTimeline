package com.sam.gogotimeline.ui.timeline

/**
 * One task as the scheduler shows it. List order is row order. All-day tasks arrive already converted
 * to local 00:00–00:00 for the current zone; [isAllDay] only picks the text format.
 */
data class TimelineTask(
    val id: Long,
    val title: String,
    val start: Long,
    val end: Long,
    val colorIndex: Int,
    val isAllDay: Boolean,
)
