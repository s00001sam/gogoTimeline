package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The scheduler: a fixed corner, a header pinned to the top, and a body below it. Positions are
 * computed here instead of using built-in scrolling; scroll values are read only while drawing.
 */
@Composable
fun TimelineScheduler(
    modifier: Modifier = Modifier,
    state: TimelineState,
    calendar: CalendarContext,
    leftPaneWidth: Dp,
    widthMultiplier: Float = TimeConst.WIDTH_MULTIPLIER_DEFAULT,
    onZoomChange: (ZoomLevel) -> Unit = {},
) {
    LaunchedEffect(calendar) { state.calendar = calendar }
    LaunchedEffect(widthMultiplier) { state.widthMultiplier = widthMultiplier }
    val currentOnZoomChange = rememberUpdatedState(onZoomChange)
    val now = rememberNow()
    val tickCache = remember { TickCache() }
    val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_MEASURER_CACHE_SIZE)
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = GogoTimelineTheme.extendedColors
    val topStyle = MaterialTheme.typography.labelMedium.copy(color = colorScheme.onSurface)
    val bottomStyle = MaterialTheme.typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
    val headerHeight = (LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp

    Layout(
        modifier = modifier
            .background(colorScheme.background)
            .clipToBounds()
            .timelineGestures(state, currentOnZoomChange),
        contents = listOf(
            { Box(Modifier.background(colorScheme.surface)) },
            {
                Canvas(Modifier) {
                    val scale = state.scale()
                    val ticks = tickCache.ticks(scale.visibleTimeRange(), state.unit, state.dpPerUnit, state.calendar)
                    drawHeader(ticks, scale, textMeasurer, topStyle, bottomStyle, extendedColors.gridMajor)
                    drawNowDot(scale.timeToX(now.value), extendedColors.nowLine)
                }
            },
            {
                Canvas(Modifier) {
                    val scale = state.scale()
                    val ticks = tickCache.ticks(scale.visibleTimeRange(), state.unit, state.dpPerUnit, state.calendar)
                    drawGridLines(ticks, scale, extendedColors.gridMinor, extendedColors.gridMajor)
                    drawNowLine(scale.timeToX(now.value), extendedColors.nowLine)
                }
            },
        ),
    ) { (corner, header, body), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val left = leftPaneWidth.roundToPx().coerceAtMost(width)
        val top = headerHeight.roundToPx().coerceAtMost(height)
        val axisWidth = width - left
        val bodyHeight = height - top
        state.updateLayout(
            density = density,
            axisLeftPx = left.toFloat(),
            viewportWidthPx = axisWidth.toFloat(),
            viewportHeightPx = bodyHeight.toFloat(),
            rowCount = 0,
            rowHeightPx = 0f,
        )
        val cornerPlaceable = corner.first().measure(Constraints.fixed(left, top))
        val headerPlaceable = header.first().measure(Constraints.fixed(axisWidth, top))
        val bodyPlaceable = body.first().measure(Constraints.fixed(axisWidth, bodyHeight))
        layout(width, height) {
            bodyPlaceable.place(left, top)
            headerPlaceable.place(left, 0)
            cornerPlaceable.place(0, 0)
        }
    }
}

private const val TEXT_MEASURER_CACHE_SIZE = 64

private val PreviewCalendar = CalendarContext(ZoneId.of("Asia/Taipei"), DayOfWeek.SUNDAY)

@Composable
private fun SchedulerPreview(unit: TimeUnit) {
    val center = LocalDateTime.of(2026, 10, 30, 12, 0).atZone(PreviewCalendar.zone).toInstant().toEpochMilli()
    PreviewSurface {
        TimelineScheduler(
            modifier = Modifier.fillMaxSize(),
            state = remember {
                TimelineState(
                    initialCenterTime = center.toDouble(),
                    initialUnit = unit,
                    initialDpPerUnit = unit.standardWidthDp,
                    calendar = PreviewCalendar,
                )
            },
            calendar = PreviewCalendar,
            leftPaneWidth = (LayoutConst.NAME_COLUMN_PHONE_EXPANDED_DP + LayoutConst.TIME_COLUMN_WIDTH_DP).dp,
        )
    }
}

@ThemePreviews
@Composable
private fun HourPreview() = SchedulerPreview(TimeUnit.HOUR)

@ThemePreviews
@Composable
private fun DayPreview() = SchedulerPreview(TimeUnit.DAY)

@ThemePreviews
@Composable
private fun WeekPreview() = SchedulerPreview(TimeUnit.WEEK)

@ThemePreviews
@Composable
private fun MonthPreview() = SchedulerPreview(TimeUnit.MONTH)
