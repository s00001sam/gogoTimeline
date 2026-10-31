package com.sam.gogotimeline.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.time.SnapInterval

/** A snap interval in its largest whole unit: `15 minutes`, `3 hours`, `1 day`, `1 week`. */
@Composable
fun snapIntervalLabel(interval: SnapInterval): String {
    val minutes = interval.minutes
    val (pluralRes, count) = when {
        minutes % SnapInterval.WEEK_MINUTES == 0 -> R.plurals.snap_weeks to minutes / SnapInterval.WEEK_MINUTES
        minutes % SnapInterval.DAY_MINUTES == 0 -> R.plurals.snap_days to minutes / SnapInterval.DAY_MINUTES
        minutes % MINUTES_PER_HOUR == 0 -> R.plurals.snap_hours to minutes / MINUTES_PER_HOUR
        else -> R.plurals.snap_minutes to minutes
    }
    return pluralStringResource(pluralRes, count, count)
}

private const val MINUTES_PER_HOUR = 60
