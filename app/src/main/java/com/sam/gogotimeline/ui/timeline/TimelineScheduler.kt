package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskTimeFormat
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import com.sam.gogotimeline.ui.timeline.preview.FakeTimelineData
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * The scheduler: a fixed corner, a header pinned to the top, a left pane, and task bars, laid out here
 * instead of with built-in scrolling. Only visible rows (plus a buffer) are composed; scrolling re-places
 * them, and recomposition happens only when the visible row range changes.
 *
 * Z-order, bottom to top: grid → bars → "now" line → left pane background → left pane rows → header →
 * corner → empty state.
 */
@Composable
fun TimelineScheduler(
    modifier: Modifier = Modifier,
    state: TimelineState,
    calendar: CalendarContext,
    tasks: List<TimelineTask>,
    leftPaneWidth: Dp,
    leftPaneExpanded: Boolean,
    onToggleLeftPane: () -> Unit,
    rowHeight: Dp = LayoutConst.ROW_HEIGHT_DEFAULT_DP.dp,
    selectedTaskId: Long? = null,
    widthMultiplier: Float = TimeConst.WIDTH_MULTIPLIER_DEFAULT,
    onZoomChange: (ZoomLevel) -> Unit = {},
    onNewTask: () -> Unit = {},
) {
    LaunchedEffect(calendar) { state.calendar = calendar }
    LaunchedEffect(widthMultiplier) { state.widthMultiplier = widthMultiplier }
    val currentOnZoomChange = rememberUpdatedState(onZoomChange)
    val now = rememberNow()
    // Only the date matters for "is this year", so bar labels do not recompose every minute.
    val today by remember(calendar) { derivedStateOf { calendar.startOfDay(calendar.localDate(now.value)) } }
    val tickCache = remember { TickCache() }
    val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_MEASURER_CACHE_SIZE)
    val styles = rememberTimelineTextStyles()
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = GogoTimelineTheme.extendedColors
    val topStyle = styles.tickTop.copy(color = colorScheme.onSurface)
    val bottomStyle = styles.tickBottom.copy(color = colorScheme.onSurfaceVariant)
    val visibleRows by remember(state) { derivedStateOf { state.visibleRowRange() } }
    val visibleTasks = visibleRows.mapNotNull { index -> tasks.getOrNull(index)?.let { index to it } }
    val headerHeight = (LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp

    Layout(
        modifier = modifier
            .background(colorScheme.background)
            .clipToBounds()
            .timelineGestures(state, currentOnZoomChange),
        contents = listOf(
            {
                Canvas(Modifier.clipToBounds()) {
                    val scale = state.scale()
                    val ticks = tickCache.ticks(scale.visibleTimeRange(), state.unit, state.dpPerUnit, state.calendar)
                    drawGridLines(ticks, scale, extendedColors.gridMinor, extendedColors.gridMajor)
                    val rowPx = rowHeight.toPx()
                    val stroke = LayoutConst.GRID_LINE_WIDTH_DP.dp.toPx()
                    visibleRows.forEach { index ->
                        val y = (index + 1) * rowPx - state.scrollY
                        drawLine(extendedColors.gridMinor, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                }
            },
            {
                visibleTasks.forEach { (index, task) ->
                    key(task.id) {
                        TaskBar(
                            modifier = Modifier.layoutId(index),
                            task = task,
                            text = remember(task, today, calendar) {
                                TaskTimeFormat.barText(TimeRange(task.start, task.end), task.isAllDay, today, calendar)
                            },
                            selected = task.id == selectedTaskId,
                            textMeasurer = textMeasurer,
                            style = styles.barText,
                            barLeftPx = { barLeftPx(state, task) },
                            axisWidthPx = { state.viewportWidthPx },
                        )
                    }
                }
            },
            {
                Canvas(Modifier.clipToBounds()) {
                    drawNowLine(state.timeToX(now.value.toDouble()), extendedColors.nowLine)
                }
            },
            { LeftPaneBackground() },
            {
                visibleTasks.forEach { (index, task) ->
                    key(task.id) {
                        LeftPaneRow(
                            modifier = Modifier.layoutId(index),
                            title = task.title,
                            styles = styles,
                            selected = task.id == selectedTaskId,
                        )
                    }
                }
            },
            {
                LeftPaneCorner(
                    expanded = leftPaneExpanded,
                    onToggle = onToggleLeftPane,
                )
            },
            {
                Canvas(Modifier.clipToBounds()) {
                    drawRect(colorScheme.background)
                    val scale = state.scale()
                    val ticks = tickCache.ticks(scale.visibleTimeRange(), state.unit, state.dpPerUnit, state.calendar)
                    drawHeader(ticks, scale, textMeasurer, topStyle, bottomStyle, extendedColors.gridMajor)
                    drawNowDot(scale.timeToX(now.value), extendedColors.nowLine)
                }
            },
            { if (tasks.isEmpty()) EmptyState(onNewTask = onNewTask) },
        ),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val left = leftPaneWidth.roundToPx().coerceAtMost(width)
        val top = headerHeight.roundToPx().coerceAtMost(height)
        val axisWidth = width - left
        val bodyHeight = height - top
        val rowPx = rowHeight.toPx()
        state.updateLayout(
            density = density,
            axisLeftPx = left.toFloat(),
            viewportWidthPx = axisWidth.toFloat(),
            viewportHeightPx = bodyHeight.toFloat(),
            rowCount = tasks.size,
            rowHeightPx = rowPx,
        )
        val bodyConstraints = Constraints.fixed(axisWidth, bodyHeight)
        val background = measurables[0].first().measure(bodyConstraints)
        val barPadding = LayoutConst.BAR_VERTICAL_PADDING_DP.dp.toPx()
        val barHeight = (rowPx - 2 * barPadding).roundToInt().coerceAtLeast(0)
        val bars = measurables[1].map { measurable ->
            val task = tasks[measurable.layoutId as Int]
            val barLeft = barLeftPx(state, task)
            val barRight = minOf(state.timeToX(task.end.toDouble()), axisWidth * BAR_CLIP_WIDTHS)
            val barWidth = (barRight - barLeft).roundToInt().coerceAtLeast(1)
            Triple(measurable.layoutId as Int, barLeft, measurable.measure(Constraints.fixed(barWidth, barHeight)))
        }
        val nowLine = measurables[2].first().measure(bodyConstraints)
        val paneBackground = measurables[3].first().measure(Constraints.fixed(left, bodyHeight))
        val rowConstraints = Constraints.fixed(left, rowPx.roundToInt())
        val rows = measurables[4].map { (it.layoutId as Int) to it.measure(rowConstraints) }
        val corner = measurables[5].first().measure(Constraints.fixed(left, top))
        val header = measurables[6].first().measure(Constraints.fixed(axisWidth, top))
        val empty = measurables[7].firstOrNull()?.measure(Constraints(maxWidth = axisWidth, maxHeight = bodyHeight))

        layout(width, height) {
            fun rowTop(index: Int): Float = top + index * rowPx - state.scrollY
            background.place(left, top)
            bars.forEach { (index, barLeft, placeable) ->
                placeable.place(left + barLeft.roundToInt(), (rowTop(index) + barPadding).roundToInt())
            }
            nowLine.place(left, top)
            paneBackground.place(0, top)
            rows.forEach { (index, placeable) -> placeable.place(0, rowTop(index).roundToInt()) }
            // The corner goes above the header so nothing from the header can show over it.
            header.place(left, 0)
            corner.place(0, 0)
            empty?.place(left + (axisWidth - empty.width) / 2, top + (bodyHeight - empty.height) / 2)
        }
    }
}

// Very long bars are cut to one viewport width beyond each side, so a year-long bar at Hour zoom stays
// within Compose's size limits.
private const val BAR_CLIP_WIDTHS = 2f

private const val TEXT_MEASURER_CACHE_SIZE = 64

private fun barLeftPx(
    state: TimelineState,
    task: TimelineTask,
): Float = maxOf(state.timeToX(task.start.toDouble()), -state.viewportWidthPx)

private val PreviewCalendar = CalendarContext(ZoneId.of("Asia/Taipei"), DayOfWeek.SUNDAY)

private val PreviewNow = LocalDateTime.of(2026, 10, 30, 12, 0).atZone(PreviewCalendar.zone).toInstant().toEpochMilli()

@Composable
private fun SchedulerPreview(
    unit: TimeUnit = TimeUnit.DAY,
    tasks: List<TimelineTask> = FakeTimelineData.tasks(PreviewNow, PreviewCalendar),
    expanded: Boolean = false,
    selectedTaskId: Long? = null,
) {
    PreviewSurface {
        TimelineScheduler(
            modifier = Modifier.fillMaxSize(),
            state = remember {
                TimelineState(
                    initialCenterTime = PreviewNow.toDouble(),
                    initialUnit = unit,
                    initialDpPerUnit = unit.standardWidthDp,
                    calendar = PreviewCalendar,
                )
            },
            calendar = PreviewCalendar,
            tasks = tasks,
            leftPaneWidth = leftPaneWidth(isTablet = false, expanded = expanded),
            leftPaneExpanded = expanded,
            onToggleLeftPane = {},
            selectedTaskId = selectedTaskId,
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

@ThemePreviews
@Composable
private fun ExpandedLeftPanePreview() = SchedulerPreview(expanded = true)

@ThemePreviews
@Composable
private fun SelectedShortTaskPreview() = SchedulerPreview(TimeUnit.HOUR, selectedTaskId = 2L)

@ThemePreviews
@Composable
private fun EmptyBoardPreview() = SchedulerPreview(tasks = emptyList())
