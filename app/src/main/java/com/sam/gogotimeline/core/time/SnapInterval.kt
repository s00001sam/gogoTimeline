package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst

@JvmInline
value class SnapInterval(val minutes: Int) {
    val millis: Long get() = minutes * 60_000L

    companion object {
        const val DAY_MINUTES = 1_440
        const val WEEK_MINUTES = 10_080

        /** What the settings page offers for [unit] (spec 6.5), finest first. */
        fun options(unit: TimeUnit): List<SnapInterval> = when (unit) {
            TimeUnit.HOUR -> listOf(15, 30, 60)
            TimeUnit.DAY -> listOf(60, 180, 360, 720)
            TimeUnit.WEEK -> listOf(720, DAY_MINUTES)
            TimeUnit.MONTH -> listOf(DAY_MINUTES, WEEK_MINUTES)
        }.map(::SnapInterval)

        fun default(unit: TimeUnit): SnapInterval = SnapInterval(
            when (unit) {
                TimeUnit.HOUR -> TimeConst.SNAP_HOUR_DEFAULT_MINUTES
                TimeUnit.DAY -> TimeConst.SNAP_DAY_DEFAULT_MINUTES
                TimeUnit.WEEK -> TimeConst.SNAP_WEEK_DEFAULT_MINUTES
                TimeUnit.MONTH -> TimeConst.SNAP_MONTH_DEFAULT_MINUTES
            },
        )
    }
}
