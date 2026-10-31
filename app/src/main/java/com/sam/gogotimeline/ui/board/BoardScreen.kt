package com.sam.gogotimeline.ui.board

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.ui.isTabletLayout
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.timeline.TimelineCallbacks
import com.sam.gogotimeline.ui.timeline.TimelineScheduler
import com.sam.gogotimeline.ui.timeline.TimelineState
import com.sam.gogotimeline.ui.timeline.TimelineTask
import com.sam.gogotimeline.ui.timeline.leftPaneWidth
import com.sam.gogotimeline.ui.timeline.preview.FakeTimelineData
import com.sam.gogotimeline.ui.timeline.rememberCalendarContext
import com.sam.gogotimeline.ui.timeline.rememberNow
import com.sam.gogotimeline.ui.timeline.rememberTimelineState
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The board screen: reads the board from [BoardViewModel] and keeps the viewport in the scheduler's own state. */
@Composable
fun BoardScreen(
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState as? BoardUiState.Ready
    val settings = ready?.settings ?: TimelineSettings.DEFAULT
    val calendar = rememberCalendarContext()
    val state = rememberTimelineState(
        initialCenterTime = System.currentTimeMillis().toDouble(),
        initialUnit = settings.zoom.unit,
        initialDpPerUnit = settings.zoom.dpPerUnit,
        calendar = calendar,
    )
    val draft = ready?.edit?.draft
    val tasks = remember(ready?.tasks, draft, calendar) {
        tasksWithDraft(ready?.tasks.orEmpty(), draft).map { it.toTimelineTask(calendar) }
    }
    val now = rememberNow()
    val todayDayOfMonth by remember(calendar) { derivedStateOf { calendar.localDate(now.value).dayOfMonth } }
    val scope = rememberCoroutineScope()
    val marginPx = with(LocalDensity.current) { LayoutConst.SCROLL_TO_TASK_MARGIN_DP.dp.toPx() }

    // Opens on the anchor once; after rotation the saved viewport takes over.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready != null) {
        if (ready == null || opened) return@LaunchedEffect
        snapshotFlow { state.viewportWidthPx }.first { it > 0f }
        val position = openingPosition(tasks, ready.board.anchorTaskId, System.currentTimeMillis())
        val x = if (position.centered) state.viewportWidthPx / 2 else marginPx
        state.jumpTo(ready.settings.zoom, position.rowIndex, position.time.toDouble(), x)
        opened = true
    }

    BackHandler(enabled = ready?.edit != null) { viewModel.onBack() }

    val callbacks = remember(viewModel, calendar) {
        TimelineCallbacks(
            onTaskTap = viewModel::onTaskTap,
            onEmptyTap = viewModel::onEmptyTap,
            // A drag that lands on full local days becomes all-day; otherwise it stops being all-day (spec 7).
            onTaskTimeChange = { id, start, end ->
                viewModel.onTaskScheduleChange(id, TaskSchedule.from(TimeRange(start, end), calendar))
            },
            onTaskMove = viewModel::onTaskMove,
            onVerticalScrollSettled = viewModel::onVerticalScrollSettled,
        )
    }
    BoardContent(
        modifier = modifier,
        boardName = ready?.board?.name.orEmpty(),
        state = state,
        calendar = calendar,
        tasks = tasks,
        settings = settings,
        selectedTaskId = ready?.selectedTaskId,
        isLoading = uiState is BoardUiState.Loading,
        isError = uiState is BoardUiState.Error,
        callbacks = callbacks,
        onToggleLeftPane = viewModel::onToggleLeftPane,
        onRetry = viewModel::retry,
        todayDayOfMonth = todayDayOfMonth,
        onGoToToday = { scope.launch { state.animateCenterTo(System.currentTimeMillis().toDouble()) } },
        onZoomChange = viewModel::onZoomChange,
    )
    val editStatus = ready?.editStatus
    if (editStatus?.confirmingClose == true) {
        SaveChangesDialog(
            isSaving = editStatus.isSaving,
            saveFailed = editStatus.saveFailed,
            onSave = viewModel::onSave,
            onDiscard = viewModel::onDiscard,
            onCancel = viewModel::onCancelClose,
        )
    }
}

/** While loading or after an error, the scheduler is a skeleton under an overlay that blocks touches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardContent(
    modifier: Modifier = Modifier,
    boardName: String,
    state: TimelineState,
    calendar: CalendarContext,
    tasks: List<TimelineTask>,
    settings: TimelineSettings,
    selectedTaskId: Long?,
    isLoading: Boolean,
    isError: Boolean,
    callbacks: TimelineCallbacks,
    onToggleLeftPane: () -> Unit,
    onRetry: () -> Unit,
    todayDayOfMonth: Int,
    onGoToToday: () -> Unit,
    onZoomChange: (ZoomLevel) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val interactive = !isLoading && !isError
    val paneWidth by animateDpAsState(
        targetValue = leftPaneWidth(isTabletLayout(), settings.leftPaneExpanded),
        animationSpec = tween(LayoutConst.LEFT_PANE_ANIMATION_MILLIS),
        label = "leftPaneWidth",
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(boardName, style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (interactive) {
                        TodayButton(dayOfMonth = todayDayOfMonth, onClick = onGoToToday)
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val target = ZoomRules.unitButtonTarget(
                                        state.unit,
                                        state.dpPerUnit,
                                        state.widthMultiplier,
                                    )
                                    state.animateZoomTo(target)
                                    onZoomChange(target)
                                }
                            },
                        ) {
                            Text(stringResource(state.unit.labelRes()))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            TimelineScheduler(
                modifier = Modifier.fillMaxSize(),
                state = state,
                calendar = calendar,
                tasks = tasks,
                leftPaneWidth = paneWidth,
                leftPaneExpanded = settings.leftPaneExpanded,
                onToggleLeftPane = onToggleLeftPane,
                rowHeight = settings.rowHeightDp.dp,
                selectedTaskId = selectedTaskId,
                snapIntervals = settings.snapIntervals,
                widthMultiplier = settings.widthMultiplier,
                callbacks = callbacks,
                onZoomChange = onZoomChange,
                showEmptyState = interactive,
            )
            if (!interactive) {
                StatusOverlay(isError = isError, onRetry = onRetry)
            }
        }
    }
}

/** Blocks touches on the skeleton and shows a spinner, or the error with Retry. Text follows the system font size. */
@Composable
private fun StatusOverlay(
    isError: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isError) {
            Column(
                modifier = Modifier.padding(LayoutConst.BOARD_STATUS_SPACING_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LayoutConst.BOARD_STATUS_SPACING_DP.dp),
            ) {
                Text(
                    text = stringResource(R.string.load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

/**
 * Today's day of month in a small square, like Google Calendar. The number has a fixed size because the
 * box is fixed and acts as an icon; screen readers hear only "Go to today".
 */
@Composable
private fun TodayButton(
    dayOfMonth: Int,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.go_to_today)
    val color = MaterialTheme.colorScheme.onSurface
    val textSize = with(LocalDensity.current) { LayoutConst.TODAY_BOX_TEXT_DP.dp.toSp() }
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .size(LayoutConst.TODAY_BOX_SIZE_DP.dp)
                .border(
                    LayoutConst.TODAY_BOX_BORDER_DP.dp,
                    color,
                    RoundedCornerShape(LayoutConst.TODAY_BOX_CORNER_DP.dp),
                )
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayOfMonth.toString(),
                color = color,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = textSize, lineHeight = textSize),
            )
        }
    }
}

@StringRes
private fun TimeUnit.labelRes(): Int = when (this) {
    TimeUnit.HOUR -> R.string.unit_hour
    TimeUnit.DAY -> R.string.unit_day
    TimeUnit.WEEK -> R.string.unit_week
    TimeUnit.MONTH -> R.string.unit_month
}

private val previewCalendar = CalendarContext(ZoneId.of("Asia/Taipei"), DayOfWeek.SUNDAY)
private val previewNow = LocalDateTime.of(2026, 10, 30, 12, 0).atZone(previewCalendar.zone).toInstant().toEpochMilli()

@Composable
private fun BoardPreview(
    tasks: List<TimelineTask>,
    selectedTaskId: Long? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    PreviewSurface {
        BoardContent(
            boardName = if (isLoading || isError) "" else "Sample Project",
            state = remember {
                TimelineState(
                    initialCenterTime = previewNow.toDouble(),
                    initialUnit = TimeUnit.DAY,
                    initialDpPerUnit = TimeUnit.DAY.standardWidthDp,
                    calendar = previewCalendar,
                )
            },
            calendar = previewCalendar,
            tasks = tasks,
            settings = TimelineSettings.DEFAULT,
            selectedTaskId = selectedTaskId,
            isLoading = isLoading,
            isError = isError,
            callbacks = TimelineCallbacks(),
            onToggleLeftPane = {},
            onRetry = {},
            todayDayOfMonth = 30,
            onGoToToday = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BoardReadyPreview() {
    BoardPreview(tasks = FakeTimelineData.tasks(previewNow, previewCalendar), selectedTaskId = 1L)
}

@ThemePreviews
@Composable
private fun BoardLoadingPreview() {
    BoardPreview(tasks = emptyList(), isLoading = true)
}

@ThemePreviews
@Composable
private fun BoardErrorPreview() {
    BoardPreview(tasks = emptyList(), isError = true)
}
