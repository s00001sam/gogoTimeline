package com.sam.gogotimeline.core.time

/** Start-inclusive, end-exclusive interval in epoch milliseconds. */
data class TimeRange(
    val start: Long,
    val end: Long,
) {
    val duration: Long get() = end - start
}
