package com.sam.gogotimeline.ui.timeline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.DragCalc
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TaskTimeFormat
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import com.sam.gogotimeline.ui.theme.SnapHintBackground
import com.sam.gogotimeline.ui.theme.SnapHintText
import com.sam.gogotimeline.ui.timeline.preview.FakeTimelineData
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

/**
 * The scheduler: a fixed corner, a header pinned to the top, a left pane, and task bars, laid out here
 * instead of with built-in scrolling. Only visible rows (plus a buffer, plus a dragged row) are composed;
 * scrolling re-places them. It never changes task data itself: taps and drags are reported through
 * [callbacks], and the owner passes the updated tasks back in.
 *
 * Z-order, bottom to top: grid → bars → "now" line → left pane background → left pane rows → header →
 * corner → drag time label → snap hint → empty state. A dragged row is drawn last within its layer.
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
    snapIntervals: Map<TimeUnit, SnapInterval> = DefaultSnapIntervals,
    widthMultiplier: Float = TimeConst.WIDTH_MULTIPLIER_DEFAULT,
    callbacks: TimelineCallbacks = TimelineCallbacks(),
    onZoomChange: (ZoomLevel) -> Unit = {},
    onNewTask: () -> Unit = {},
    showEmptyState: Boolean = true,
) {
    LaunchedEffect(calendar) { state.calendar = calendar }
    LaunchedEffect(widthMultiplier) { state.widthMultiplier = widthMultiplier }
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
    val dragLabelStyle = styles.barText.copy(color = colorScheme.surface)
    val headerHeight = (LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val localDensity = LocalDensity.current
    val interaction = remember(state) { TimelineInteraction(state, scope) }
    val barTextOf: (TimelineTask) -> String = { task ->
        TaskTimeFormat.barText(TimeRange(task.start, task.end), task.isAllDay, today, calendar)
    }
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    interaction.update(
        tasks = tasks,
        selectedTaskId = selectedTaskId,
        snap = snapIntervals[state.unit] ?: SnapInterval.default(state.unit),
        sizes = with(localDensity) {
            InteractionSizes(
                headerHeight = headerHeight.toPx(),
                rowHeight = rowHeight.toPx(),
                handleOutside = LayoutConst.HANDLE_OUTSIDE_DP.dp.toPx(),
                handleInside = LayoutConst.HANDLE_INSIDE_DP.dp.toPx(),
                labelGap = LayoutConst.TICK_LABEL_PADDING_DP.dp.toPx(),
                labelPaddingX = LayoutConst.BAR_LABEL_PADDING_HORIZONTAL_DP.dp.toPx(),
                scrollToTaskMargin = LayoutConst.SCROLL_TO_TASK_MARGIN_DP.dp.toPx(),
            )
        },
        callbacks = callbacks.copy(onZoomEnd = { currentOnZoomChange(state.zoomLevel) }),
        labelText = barTextOf,
        labelWidth = { textMeasurer.measure(it, styles.barText).size.width.toFloat() },
        haptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
    )
    SideEffect { interaction.clearReleasedIfApplied() }
    val gestureHandler = rememberUpdatedState<TimelineGestureHandler>(interaction)

    val drag = interaction.drag
    val dragging = drag != null
    LaunchedEffect(dragging) {
        if (dragging) {
            with(localDensity) {
                interaction.autoScroll(
                    zone = LayoutConst.EDGE_AUTO_SCROLL_ZONE_DP.dp.toPx(),
                    maxSpeed = LayoutConst.MAX_AUTO_SCROLL_SPEED_DP_PER_SECOND.dp.toPx(),
                )
            }
        }
    }

    val rowTasks = interaction.rowTasks()
    val visibleRows by remember(state) { derivedStateOf { state.visibleRowRange() } }
    // Composing a row costs far more than drawing ticks, so rows up to a page away are prepared one per frame
    // before they scroll in; the loop stops once the page is filled.
    var preparedRows by remember(state) { mutableStateOf(IntRange.EMPTY) }
    val rowCount by rememberUpdatedState(rowTasks.size)
    LaunchedEffect(state) {
        snapshotFlow { visibleRows to rowCount }.collectLatest { (visible, count) ->
            val target = GestureMath.preparedRowRange(visible, count)
            while (true) {
                val next = GestureMath.nextPreparedRows(preparedRows, visible, target)
                if (next == preparedRows) break
                preparedRows = next
                withFrameNanos {}
            }
        }
    }
    val composedRows by remember(state) { derivedStateOf { GestureMath.composedRows(visibleRows, preparedRows) } }
    // The dragged row stays composed even when it leaves the visible range.
    val rowIndices = (composedRows.toList() + listOfNotNull(drag?.originalIndex))
        .distinct()
        .filter { it in rowTasks.indices }
        .sortedBy { it == drag?.originalIndex }
    val reorderTarget = interaction.reorderTarget()
    val shownTasks = rowIndices.associateWith { index ->
        val task = rowTasks[index]
        val range = interaction.displayRange(task)
        task.copy(start = range.start, end = range.end)
    }
    val rowPx = with(localDensity) { rowHeight.toPx() }
    val snapHintText = snapHintText(interaction.grid().interval)

    Layout(
        modifier = modifier
            .background(colorScheme.background)
            .clipToBounds()
            .timelineGestures(state, gestureHandler),
        contents = listOf(
            {
                Canvas(Modifier.clipToBounds()) {
                    val scale = state.scale()
                    val ticks = tickCache.ticks(scale.visibleTimeRange(), state.unit, state.dpPerUnit, state.calendar)
                    drawGridLines(ticks, scale, extendedColors.gridMinor, extendedColors.gridMajor)
                    drawRowLines(state.scrollY, rowPx, extendedColors.gridMinor)
                }
            },
            {
                rowIndices.forEach { index ->
                    val task = shownTasks.getValue(index)
                    key(task.id) {
                        ScheduledBar(
                            modifier = Modifier
                                .layoutId(index)
                                .dimmedWhileEditing(selectedTaskId, task.id),
                            task = task,
                            index = index,
                            selected = task.id == selectedTaskId,
                            lifted = task.id == interaction.liftedTaskId,
                            today = today,
                            calendar = calendar,
                            drag = drag,
                            reorderTarget = reorderTarget,
                            rowPx = rowPx,
                            interaction = interaction,
                            state = state,
                            textMeasurer = textMeasurer,
                            style = styles.barText,
                        )
                    }
                }
            },
            {
                Canvas(Modifier.clipToBounds()) {
                    drawNowLine(state.timeToX(now.value.toDouble()), extendedColors.nowLine)
                }
            },
            { LeftPaneBackground(scrollY = { state.scrollY }, rowPx = rowPx) },
            {
                rowIndices.forEach { index ->
                    val task = shownTasks.getValue(index)
                    key(task.id) {
                        ScheduledName(
                            modifier = Modifier
                                .layoutId(index)
                                .dimmedWhileEditing(selectedTaskId, task.id),
                            title = task.title,
                            index = index,
                            selected = task.id == selectedTaskId,
                            lifted = task.id == interaction.liftedTaskId,
                            drag = drag,
                            reorderTarget = reorderTarget,
                            rowPx = rowPx,
                            interaction = interaction,
                            styles = styles,
                        )
                    }
                }
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
            {
                LeftPaneCorner(
                    expanded = leftPaneExpanded,
                    onToggle = onToggleLeftPane,
                )
            },
            {
                Canvas(Modifier.clipToBounds()) {
                    val session = interaction.drag ?: return@Canvas
                    val preview = interaction.previewRange() ?: return@Canvas
                    val layout = textMeasurer.measure(
                        session.dragTimeLabel(preview, interaction.grid(), state.calendar),
                        dragLabelStyle,
                    )
                    val paddingX = LayoutConst.TIME_LABEL_PADDING_HORIZONTAL_DP.dp.toPx()
                    val paddingY = LayoutConst.TIME_LABEL_PADDING_VERTICAL_DP.dp.toPx()
                    val gap = LayoutConst.TIME_LABEL_GAP_DP.dp.toPx()
                    val labelSize = Size(layout.size.width + 2 * paddingX, layout.size.height + 2 * paddingY)
                    val barPadding = LayoutConst.BAR_VERTICAL_PADDING_DP.dp.toPx()
                    val barTop = session.originalIndex * rowPx - state.scrollY + barPadding
                    val above = barTop - gap - labelSize.height
                    val top = if (above >= 0f) above else barTop + rowPx - 2 * barPadding + gap
                    val left = (interaction.pointer.x - state.axisLeftPx - labelSize.width / 2)
                        .coerceIn(0f, (size.width - labelSize.width).coerceAtLeast(0f))
                    drawRoundRect(
                        color = colorScheme.onSurface,
                        topLeft = Offset(left, top),
                        size = labelSize,
                        cornerRadius = CornerRadius(LayoutConst.TIME_LABEL_CORNER_DP.dp.toPx()),
                    )
                    drawText(layout, topLeft = Offset(left + paddingX, top + paddingY))
                }
            },
            {
                Canvas(Modifier.clipToBounds()) {
                    // Hidden while dragging, where the live time label takes its place.
                    if (interaction.drag != null) return@Canvas
                    val index = rowTasks.indexOfFirst { it.id == selectedTaskId }
                    if (index < 0) return@Canvas
                    val range = interaction.displayRange(rowTasks[index])
                    if (DragCalc.fitsGrid(range, interaction.grid())) return@Canvas
                    drawSnapHint(
                        text = snapHintText,
                        barLeft = state.timeToX(range.start.toDouble()),
                        barRight = state.timeToX(range.end.toDouble()),
                        barTop = index * rowPx - state.scrollY + LayoutConst.BAR_VERTICAL_PADDING_DP.dp.toPx(),
                        barHeight = rowPx - 2 * LayoutConst.BAR_VERTICAL_PADDING_DP.dp.toPx(),
                        textMeasurer = textMeasurer,
                        style = styles.snapHint,
                    )
                }
            },
            { if (showEmptyState && tasks.isEmpty()) EmptyState(onNewTask = onNewTask) },
        ),
    ) { measurables, constraints ->
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
            rowCount = tasks.size,
            rowHeightPx = rowPx,
        )
        val bodyConstraints = Constraints.fixed(axisWidth, bodyHeight)
        val background = measurables[0].first().measure(bodyConstraints)
        val barPadding = LayoutConst.BAR_VERTICAL_PADDING_DP.dp.toPx()
        val barHeight = (rowPx - 2 * barPadding).roundToInt().coerceAtLeast(0)
        val bars = measurables[1].map { measurable ->
            val index = measurable.layoutId as Int
            val task = shownTasks.getValue(index)
            val barLeft = barLeftPx(state, task)
            val barRight = minOf(state.timeToX(task.end.toDouble()), axisWidth * LayoutConst.BAR_CLIP_WIDTHS)
            val barWidth = (barRight - barLeft).roundToInt().coerceAtLeast(1)
            Triple(index, barLeft, measurable.measure(Constraints.fixed(barWidth, barHeight)))
        }
        val nowLine = measurables[2].first().measure(bodyConstraints)
        val paneBackground = measurables[3].first().measure(Constraints.fixed(left, bodyHeight))
        val rowConstraints = Constraints.fixed(left, rowPx.roundToInt())
        val rows = measurables[4].map { (it.layoutId as Int) to it.measure(rowConstraints) }
        val header = measurables[5].first().measure(Constraints.fixed(axisWidth, top))
        val corner = measurables[6].first().measure(Constraints.fixed(left, top))
        val dragLabel = measurables[7].first().measure(bodyConstraints)
        val snapHint = measurables[8].first().measure(bodyConstraints)
        val empty = measurables[9].firstOrNull()?.measure(Constraints(maxWidth = axisWidth, maxHeight = bodyHeight))

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
            dragLabel.place(left, top)
            snapHint.place(left, top)
            empty?.place(left + (axisWidth - empty.width) / 2, top + (bodyHeight - empty.height) / 2)
        }
    }
}

private val DefaultSnapIntervals: Map<TimeUnit, SnapInterval> = TimeUnit.entries.associateWith {
    SnapInterval.default(it)
}

private const val TEXT_MEASURER_CACHE_SIZE = 64

private fun barLeftPx(
    state: TimelineState,
    task: TimelineTask,
): Float = maxOf(state.timeToX(task.start.toDouble()), -state.viewportWidthPx)

/**
 * One row's bar. Its own restart scope with inputs that do not change while scrolling, so crossing a row
 * composes only the row that appears instead of redoing every visible row.
 */
@Composable
private fun ScheduledBar(
    modifier: Modifier = Modifier,
    task: TimelineTask,
    index: Int,
    selected: Boolean,
    lifted: Boolean,
    today: Long,
    calendar: CalendarContext,
    drag: DragSession?,
    reorderTarget: Int?,
    rowPx: Float,
    interaction: TimelineInteraction,
    state: TimelineState,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    val offset = rowOffsetPx(index, drag, reorderTarget, rowPx, interaction)
    val text = remember(task, today, calendar) {
        TaskTimeFormat.barText(TimeRange(task.start, task.end), task.isAllDay, today, calendar)
    }
    TaskBar(
        modifier = modifier
            .offset { IntOffset(0, offset.roundToInt()) }
            .lifted(lifted),
        task = task,
        text = text,
        selected = selected,
        textMeasurer = textMeasurer,
        style = style,
        barLeftPx = { barLeftPx(state, task) },
        axisWidthPx = { state.viewportWidthPx },
    )
}

/** One row's name in the left pane, skipped while scrolling for the same reason as [ScheduledBar]. */
@Composable
private fun ScheduledName(
    modifier: Modifier = Modifier,
    title: String,
    index: Int,
    selected: Boolean,
    lifted: Boolean,
    drag: DragSession?,
    reorderTarget: Int?,
    rowPx: Float,
    interaction: TimelineInteraction,
    styles: TimelineTextStyles,
) {
    val offset = rowOffsetPx(index, drag, reorderTarget, rowPx, interaction)
    LeftPaneRow(
        modifier = modifier
            .offset { IntOffset(0, offset.roundToInt()) }
            .lifted(lifted),
        title = title,
        styles = styles,
        selected = selected,
    )
}

/**
 * Vertical offset of a row while reordering: the dragged row follows the finger, rows passed slide one slot
 * with a short animation, and everything snaps back to 0 the moment the drag ends so the new order never
 * shows a stale offset.
 */
@Composable
private fun rowOffsetPx(
    index: Int,
    drag: DragSession?,
    reorderTarget: Int?,
    rowPx: Float,
    interaction: TimelineInteraction,
): Float {
    val shift = remember { Animatable(0f) }
    val target = if (drag != null && reorderTarget != null) {
        reorderShift(index, drag.originalIndex, reorderTarget) * rowPx
    } else {
        0f
    }
    LaunchedEffect(target, drag != null) {
        if (drag !=
            null
        ) {
            shift.animateTo(target, tween(LayoutConst.REORDER_SHIFT_ANIMATION_MILLIS))
        } else {
            shift.snapTo(0f)
        }
    }
    return when {
        drag == null || reorderTarget == null -> 0f
        drag.originalIndex == index -> interaction.draggedRowOffset()
        else -> shift.value
    }
}

/** While a task is edited, every other row (bar, time label and left-pane cell) fades back. */
private fun Modifier.dimmedWhileEditing(
    selectedTaskId: Long?,
    taskId: Long,
): Modifier = if (selectedTaskId != null && selectedTaskId != taskId) alpha(LayoutConst.EDIT_DIM_ALPHA) else this

/** `Current snap interval: 1 day` in bold, then what resizing does. */
@Composable
private fun snapHintText(interval: SnapInterval): AnnotatedString {
    val minutes = interval.minutes
    val (pluralRes, count) = when {
        minutes % SnapInterval.WEEK_MINUTES == 0 -> R.plurals.snap_weeks to minutes / SnapInterval.WEEK_MINUTES
        minutes % SnapInterval.DAY_MINUTES == 0 -> R.plurals.snap_days to minutes / SnapInterval.DAY_MINUTES
        minutes % MINUTES_PER_HOUR == 0 -> R.plurals.snap_hours to minutes / MINUTES_PER_HOUR
        else -> R.plurals.snap_minutes to minutes
    }
    val intervalLine = stringResource(R.string.snap_hint_interval, pluralStringResource(pluralRes, count, count))
    val resizeLine = stringResource(R.string.snap_hint_resize)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(intervalLine) }
        append("\n")
        append(resizeLine)
    }
}

/**
 * The snap hint above the selected bar (below it when there is no room above), aligned with the bar's left edge and
 * kept inside the time axis. Not drawn when the bar is off screen.
 */
private fun DrawScope.drawSnapHint(
    text: AnnotatedString,
    barLeft: Float,
    barRight: Float,
    barTop: Float,
    barHeight: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    if (barRight <= 0f || barLeft >= size.width || barTop + barHeight <= 0f || barTop >= size.height) return
    val paddingX = LayoutConst.SNAP_HINT_PADDING_HORIZONTAL_DP.dp.toPx()
    val paddingY = LayoutConst.SNAP_HINT_PADDING_VERTICAL_DP.dp.toPx()
    val gap = LayoutConst.SNAP_HINT_GAP_DP.dp.toPx()
    val layout = textMeasurer.measure(
        text = text,
        style = style.copy(color = SnapHintText),
        constraints = Constraints(maxWidth = (size.width - 2 * paddingX).roundToInt().coerceAtLeast(0)),
    )
    val hintSize = Size(layout.size.width + 2 * paddingX, layout.size.height + 2 * paddingY)
    val above = barTop - gap - hintSize.height
    val top = if (above >= 0f) above else barTop + barHeight + gap
    val left = barLeft.coerceIn(0f, (size.width - hintSize.width).coerceAtLeast(0f))
    drawRoundRect(
        color = SnapHintBackground,
        topLeft = Offset(left, top),
        size = hintSize,
        cornerRadius = CornerRadius(LayoutConst.SNAP_HINT_CORNER_DP.dp.toPx()),
    )
    drawText(layout, topLeft = Offset(left + paddingX, top + paddingY))
}

private const val MINUTES_PER_HOUR = 60

/** Lifts a long-pressed row: scales up a little with a shadow, so it reads as picked up even without haptics. */
@Composable
private fun Modifier.lifted(lifted: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (lifted) LayoutConst.LONG_PRESS_LIFT_SCALE else 1f,
        animationSpec = tween(LayoutConst.LONG_PRESS_LIFT_ANIMATION_MILLIS),
        label = "lift",
    )
    val elevation = with(LocalDensity.current) { LayoutConst.DRAG_ELEVATION_DP.dp.toPx() }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        shadowElevation = elevation * (scale - 1f) / (LayoutConst.LONG_PRESS_LIFT_SCALE - 1f)
    }
}

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
private fun SelectedTaskPreview() = SchedulerPreview(TimeUnit.HOUR, selectedTaskId = 1L)

@ThemePreviews
@Composable
private fun SelectedShortTaskPreview() = SchedulerPreview(TimeUnit.HOUR, selectedTaskId = 2L)

@ThemePreviews
@Composable
private fun EmptyBoardPreview() = SchedulerPreview(tasks = emptyList())
