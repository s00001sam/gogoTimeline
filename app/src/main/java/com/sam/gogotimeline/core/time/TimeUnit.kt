package com.sam.gogotimeline.core.time

import com.sam.gogotimeline.core.TimeConst

enum class TimeUnit(
    val standardWidthDp: Float,
    // Only used to compare zoom levels across units; real lengths come from the calendar.
    val nominalDays: Double,
) {
    HOUR(TimeConst.HOUR_STANDARD_WIDTH_DP, 1.0 / 24),
    DAY(TimeConst.DAY_STANDARD_WIDTH_DP, 1.0),
    WEEK(TimeConst.WEEK_STANDARD_WIDTH_DP, 7.0),
    MONTH(TimeConst.MONTH_STANDARD_WIDTH_DP, TimeConst.MONTH_STANDARD_DAYS.toDouble()),
    ;

    /** Unit button order: Hour → Day → Week → Month → Hour. */
    fun next(): TimeUnit = entries[(ordinal + 1) % entries.size]

    fun finer(): TimeUnit? = entries.getOrNull(ordinal - 1)

    fun coarser(): TimeUnit? = entries.getOrNull(ordinal + 1)

    fun standardWidthDp(widthMultiplier: Float): Float = standardWidthDp * widthMultiplier
}
