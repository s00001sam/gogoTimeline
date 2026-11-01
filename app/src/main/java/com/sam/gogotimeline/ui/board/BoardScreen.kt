package com.sam.gogotimeline.ui.board

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.gesture.FloatRect
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.SheetTimeEdit
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TaskTimes
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.data.model.TimelineSettings
import com.sam.gogotimeline.ui.EditorLayout
import com.sam.gogotimeline.ui.editorLayout
import com.sam.gogotimeline.ui.isTabletLayout
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.sheet.Boundary
import com.sam.gogotimeline.ui.sheet.SheetMoment
import com.sam.gogotimeline.ui.sheet.TaskSheet
import com.sam.gogotimeline.ui.sheet.TaskSheetState
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The board screen: reads the board from [BoardViewModel] and keeps the viewport in the scheduler's own state. */
@Composable
fun BoardScreen(
    modifier: Modifier = Modifier,
    boardId: Long,
    openNewTask: Boolean = false,
    onNewTaskOpened: () -> Unit = {},
    onAppAction: (BoardAction) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    interactionBlocked: Boolean = false,
    viewModel: BoardViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel, boardId) { viewModel.openBoard(boardId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnAppAction by rememberUpdatedState(onAppAction)
    LaunchedEffect(viewModel) { viewModel.actions.collect { currentOnAppAction(it) } }
    val ready = uiState as? BoardUiState.Ready
    val settings = ready?.settings ?: TimelineSettings.DEFAULT
    val calendar = rememberCalendarContext()
    val state = rememberTimelineState(
        initialCenterTime = System.currentTimeMillis().toDouble(),
        initialUnit = settings.zoom.unit,
        initialDpPerUnit = settings.zoom.dpPerUnit,
        calendar = calendar,
    )
    // The bar keeps the last valid times while the sheet shows invalid ones.
    val draft = ready?.edit?.let { it.draft.copy(schedule = it.lastValidSchedule) }
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
    // A new width multiplier (or a reset) from Settings rescales the saved width; the scheduler follows it with the
    // center time unchanged. Pinch zoom saves its own width, so only the multiplier is watched.
    LaunchedEffect(ready?.settings?.widthMultiplier) {
        if (ready != null && opened) state.setZoom(ready.settings.zoom)
    }

    val sheet = remember(ready?.edit, ready?.editStatus, calendar) {
        val edit = ready?.edit ?: return@remember null
        val schedule = edit.draft.schedule
        TaskSheetState(
            title = edit.draft.title,
            notes = edit.draft.notes,
            start = SheetMoment(
                SheetTimeEdit.startDate(schedule, calendar),
                SheetTimeEdit.startTime(schedule, calendar),
            ),
            end = SheetMoment(SheetTimeEdit.endDate(schedule, calendar), SheetTimeEdit.endTime(schedule, calendar)),
            isAllDay = schedule is TaskSchedule.AllDay,
            timeError = edit.timeError,
            colorIndex = edit.draft.colorIndex,
            canSave = ready.canSave,
            isSaving = ready.editStatus.isSaving,
            // A failure from the dialog's Save shows in the dialog instead.
            saveFailed = ready.editStatus.saveFailed && !ready.editStatus.confirmingClose,
            isNew = edit.draft.isNew,
        )
    }

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
    val createNewTask = {
        val unit = state.unit
        val grid = calendar.grid(settings.snapFor(unit))
        val range = TaskTimes.newTaskRange(System.currentTimeMillis(), unit, grid, calendar)
        viewModel.onNewTask(TaskSchedule.from(range, calendar))
        val visible = state.timeToX(range.end.toDouble()) > 0f &&
            state.timeToX(range.start.toDouble()) < state.viewportWidthPx
        if (!visible) scope.launch { state.animateTimeToX(range.start.toDouble(), marginPx) }
    }
    LaunchedEffect(ready != null, openNewTask) {
        if (ready != null && openNewTask) {
            createNewTask()
            onNewTaskOpened()
        }
    }
    BoardContent(
        modifier = modifier,
        boardName = ready?.board?.name.orEmpty(),
        locked = ready?.board?.locked == true,
        onToggleLock = viewModel::onToggleLock,
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
        onSettingsClick = { viewModel.onAppAction(BoardAction.OpenSettings) },
        onRenameBoard = { viewModel.onAppAction(BoardAction.RenameBoard) },
        onDeleteBoard = { viewModel.onAppAction(BoardAction.DeleteBoard) },
        // ☰ on phones and tablets alike: it opens the modal drawer, or shows / hides the docked one (spec 9).
        showNavigation = true,
        onOpenDrawer = onOpenDrawer,
        interactionBlocked = interactionBlocked,
        // Starts at "now" floored to the current snap and lasts two of the current unit (spec 7.3); an off-screen
        // bar is scrolled to like a tapped off-screen task.
        onNewTask = createNewTask,
        sheet = sheet,
        onTitleChange = viewModel::onTitleChange,
        onNotesChange = viewModel::onNotesChange,
        // Picked dates and times are applied to the draft's current (possibly invalid) times, not the bar's.
        onDatePick = { boundary, date ->
            val schedule = ready?.edit?.draft?.schedule ?: return@BoardContent
            viewModel.onSheetScheduleChange(
                when (boundary) {
                    Boundary.START -> SheetTimeEdit.withStartDate(schedule, date, calendar)
                    Boundary.END -> SheetTimeEdit.withEndDate(schedule, date, calendar)
                },
            )
        },
        onTimePick = { boundary, time ->
            val schedule = ready?.edit?.draft?.schedule ?: return@BoardContent
            viewModel.onSheetScheduleChange(
                when (boundary) {
                    Boundary.START -> SheetTimeEdit.withStartTime(schedule, time, calendar)
                    Boundary.END -> SheetTimeEdit.withEndTime(schedule, time, calendar)
                },
            )
        },
        onAllDayChange = { viewModel.onAllDayChange(it, calendar) },
        onColorChange = viewModel::onColorChange,
        onCloseEditor = viewModel::onBack,
        onConfirmEdit = { viewModel.onConfirm(calendar) },
        onDeleteTask = viewModel::onDeleteRequest,
    )
    if (ready?.confirmingLock == true) {
        LockBoardDialog(
            boardName = ready.board.name,
            onLock = viewModel::onLockConfirm,
            onCancel = viewModel::onLockCancel,
        )
    }
    val edit = ready?.edit ?: return
    val editStatus = ready.editStatus
    if (editStatus.confirmingClose) {
        SaveChangesDialog(
            canSave = ready.canSave,
            isSaving = editStatus.isSaving,
            saveFailed = editStatus.saveFailed,
            onSave = { viewModel.onSave(calendar) },
            onDiscard = viewModel::onDiscard,
            onCancel = viewModel::onCancelDialog,
        )
    }
    if (editStatus.confirmingDelete) {
        DeleteTaskDialog(
            taskName = edit.original.title,
            isDeleting = editStatus.isDeleting,
            deleteFailed = editStatus.deleteFailed,
            onDelete = viewModel::onDeleteConfirm,
            onCancel = viewModel::onCancelDialog,
        )
    }
}

/** While loading or after an error, the scheduler is a skeleton under an overlay that blocks touches. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    locked: Boolean = false,
    onToggleLock: () -> Unit = {},
    onZoomChange: (ZoomLevel) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRenameBoard: () -> Unit = {},
    onDeleteBoard: () -> Unit = {},
    showNavigation: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    interactionBlocked: Boolean = false,
    onNewTask: () -> Unit = {},
    sheet: TaskSheetState? = null,
    onTitleChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onDatePick: (Boundary, LocalDate) -> Unit = { _, _ -> },
    onTimePick: (Boundary, LocalTime) -> Unit = { _, _ -> },
    onAllDayChange: (Boolean) -> Unit = {},
    onColorChange: (Int) -> Unit = {},
    onCloseEditor: () -> Unit = {},
    onConfirmEdit: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    hasBoard: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val interactive = hasBoard && !isLoading && !isError
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // The bottom sheet needs height; wide windows edit in a floating card and short narrow ones on a full page (7.1).
    val editorLayout = editorLayout()
    val useSheet = editorLayout == EditorLayout.BOTTOM_SHEET
    val currentUseSheet by rememberUpdatedState(useSheet)
    // The sheet stays hidden until a task is edited; it keeps the last content while it slides away.
    val currentEditing by rememberUpdatedState(sheet != null)
    val currentIsNew by rememberUpdatedState(sheet?.isNew == true)
    val currentOnCloseEditor by rememberUpdatedState(onCloseEditor)
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        // A swipe down from collapsed acts like ×: the sheet snaps back and the close flow decides (it may ask
        // Save / Discard). Dragging up expands to show the notes. Without the sheet layout it just hides.
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden && currentEditing && currentUseSheet) {
                currentOnCloseEditor()
                false
            } else {
                true
            }
        },
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    // A long press on a locked board says why nothing happens (the one hint text, spec 9); a repeat restarts it.
    val lockedMessage = stringResource(R.string.board_locked)
    var lockedHint by remember { mutableStateOf<Job?>(null) }
    val schedulerCallbacks = remember(callbacks, lockedMessage) {
        callbacks.copy(
            onLockedLongPress = {
                lockedHint?.cancel()
                lockedHint = scope.launch {
                    withTimeoutOrNull(LayoutConst.LOCKED_HINT_MILLIS) {
                        scaffoldState.snackbarHostState.showSnackbar(
                            lockedMessage,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                }
            },
        )
    }
    var shownSheet by remember { mutableStateOf(sheet) }
    if (sheet != null) shownSheet = sheet
    val editing = sheet != null
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    val sheetMeasured = sheetHeightPx > 0
    // Shows the sheet once per edit, after its collapsed part is measured. The peek height reaches the scaffold a
    // few frames later and asking before that is ignored, so it asks again each frame until the sheet is shown.
    // Later height changes (keyboard, font size, an error line) only move the anchor; the scaffold follows them
    // frame by frame, so no second animation runs.
    LaunchedEffect(editing, sheetMeasured, useSheet) {
        when {
            // No sheet, no keyboard: whatever ended editing, the text fields lose focus first.
            !editing -> {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                sheetState.hide()
            }

            // Rotated into the floating card or full page while editing: the sheet steps aside, editing goes on.
            !useSheet -> sheetState.hide()

            // A new task opens expanded (spec 7.3); an existing one collapsed.
            sheetMeasured -> repeat(LayoutConst.SHEET_SHOW_MAX_FRAMES) {
                if (sheetState.currentValue != SheetValue.Hidden) return@LaunchedEffect
                withFrameNanos {}
                if (currentIsNew) sheetState.expand() else sheetState.partialExpand()
            }
        }
    }
    // Each time the collapsed sheet appears (editing starts, or back from full screen), the selected row moves to just
    // above it, whether or not it was covered. The scheduler reaches the screen bottom like the sheet, so the sheet's
    // top is the body height minus its peek height. Keyboard height changes do not change the sheet's value, so
    // typing never scrolls.
    val rowGapPx = with(LocalDensity.current) { LayoutConst.ROW_ABOVE_SHEET_DP.dp.toPx() }
    val currentSelectedRow by rememberUpdatedState(tasks.indexOfFirst { it.id == selectedTaskId })
    val currentSheetHeightPx by rememberUpdatedState(sheetHeightPx)
    LaunchedEffect(sheetState) {
        var previous = sheetState.currentValue
        snapshotFlow { sheetState.currentValue }.collect { value ->
            val row = currentSelectedRow
            if (value == SheetValue.PartiallyExpanded && previous != value && row >= 0) {
                state.animateRowAbove(row, state.viewportHeightPx - currentSheetHeightPx, rowGapPx)
            }
            previous = value
        }
    }
    // When the floating card appears over the edited bar (fully or partly), the bar scrolls out to the left of it like
    // a tapped off-screen task, so it can still be dragged (spec 7.1). Checked once each time the card appears.
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val currentCardBounds by rememberUpdatedState(cardBounds)
    val cardShown = editorLayout == EditorLayout.FLOATING_CARD && cardBounds.width > 0f
    // Dragging a bar sideways next to the card auto-scrolls at the card's left edge.
    SideEffect {
        state.coveredRect = if (editing && cardShown) {
            FloatRect(cardBounds.left, cardBounds.top, cardBounds.right, cardBounds.bottom)
        } else {
            null
        }
    }
    val currentTasks by rememberUpdatedState(tasks)
    val density = LocalDensity.current
    val headerPx =
        with(density) { (LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp.toPx() }
    val rowPx = with(density) { settings.rowHeightDp.dp.toPx() }
    val scrollMarginPx = with(density) { LayoutConst.SCROLL_TO_TASK_MARGIN_DP.dp.toPx() }
    LaunchedEffect(editing, cardShown) {
        if (!editing || !cardShown) return@LaunchedEffect
        val row = currentSelectedRow
        val task = currentTasks.getOrNull(row) ?: return@LaunchedEffect
        val card = currentCardBounds
        val barTop = headerPx + row * rowPx - state.scrollY
        val covered = GestureMath.isCovered(
            barLeft = state.axisLeftPx + state.timeToX(task.start.toDouble()),
            barRight = state.axisLeftPx + state.timeToX(task.end.toDouble()),
            barTop = barTop,
            barBottom = barTop + rowPx,
            cover = FloatRect(card.left, card.top, card.right, card.bottom),
        )
        if (covered) state.animateTimeToX(task.start.toDouble(), scrollMarginPx)
    }
    // Going from expanded to collapsed always closes the keyboard, whether it was for the name or the notes.
    LaunchedEffect(sheetState) {
        var previous = sheetState.targetValue
        snapshotFlow { sheetState.targetValue }.collect { target ->
            if (previous == SheetValue.Expanded && target == SheetValue.PartiallyExpanded) {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
            }
            previous = target
        }
    }
    // Back: with the keyboard open only the keyboard closes; expanded collapses; collapsed acts like ×. Handled here
    // rather than left to the system, because some keyboards do not consume back and the app would close.
    val keyboardOpen = WindowInsets.isImeVisible
    BackHandler(enabled = editing) {
        when {
            keyboardOpen -> {
                keyboard?.hide()
                focusManager.clearFocus()
            }

            sheetState.currentValue == SheetValue.Expanded -> scope.launch { sheetState.partialExpand() }

            else -> onCloseEditor()
        }
    }
    val paneWidth by animateDpAsState(
        targetValue = leftPaneWidth(isTabletLayout(), settings.leftPaneExpanded),
        animationSpec = tween(LayoutConst.LEFT_PANE_ANIMATION_MILLIS),
        label = "leftPaneWidth",
    )
    // The keyboard overlays the screen instead of shrinking it: a changing screen height made the scaffold
    // re-place the sheet a frame late. The sheet lifts itself above the keyboard through its peek height.
    // The same fields outside the sheet: everything at once, no handle.
    val editorContent: @Composable (Modifier) -> Unit = { contentModifier ->
        shownSheet?.let {
            TaskSheet(
                modifier = contentModifier,
                state = it,
                expanded = true,
                focusName = sheet?.isNew == true,
                onTitleChange = onTitleChange,
                onNotesChange = onNotesChange,
                onDatePick = onDatePick,
                onTimePick = onTimePick,
                onAllDayChange = onAllDayChange,
                onColorChange = onColorChange,
                onClose = onCloseEditor,
                onConfirm = onConfirmEdit,
                onDelete = onDeleteTask,
                inSheet = false,
            )
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        BottomSheetScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = with(LocalDensity.current) { (if (useSheet) sheetHeightPx else 0).toDp() },
            sheetDragHandle = null,
            snackbarHost = { SnackbarHost(it, Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
            sheetContent = {
                if (useSheet) {
                    shownSheet?.let {
                        TaskSheet(
                            state = it,
                            expanded = sheetState.targetValue == SheetValue.Expanded ||
                                sheetState.currentValue == SheetValue.Expanded,
                            focusName = sheet?.isNew == true,
                            onTitleChange = onTitleChange,
                            onNotesChange = onNotesChange,
                            onDatePick = onDatePick,
                            onTimePick = onTimePick,
                            onAllDayChange = onAllDayChange,
                            onColorChange = onColorChange,
                            onClose = onCloseEditor,
                            onConfirm = onConfirmEdit,
                            onDelete = onDeleteTask,
                            onCollapsedHeightChange = { sheetHeightPx = it },
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        // The name shrinks to "…" first, so the lock beside it always stays visible.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                boardName,
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (interactive) {
                                LockIcon(
                                    modifier = Modifier.padding(start = LayoutConst.LOCK_ICON_GAP_DP.dp),
                                    locked = locked,
                                    onClick = onToggleLock,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (showNavigation) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_menu),
                                    contentDescription = stringResource(R.string.open_boards),
                                )
                            }
                        }
                    },
                    actions = {
                        if (interactive) {
                            // No IconButton / TextButton: their 48dp boxes spread the three apart. Each draws its own
                            // circular ripple a little beyond its content, with a fixed gap between them.
                            val gap = topBarActionGap()
                            Row(
                                modifier = Modifier.padding(end = gap),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TodayButton(dayOfMonth = todayDayOfMonth, onClick = onGoToToday)
                                Text(
                                    text = stringResource(state.unit.labelRes()),
                                    modifier = Modifier.topBarAction {
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
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                OverflowMenu(
                                    onSettingsClick = onSettingsClick,
                                    onRenameClick = onRenameBoard,
                                    onDeleteClick = onDeleteBoard,
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            // Only the top bar's padding: the bottom one reserves the sheet's peek height even while it is hidden,
            // and the sheet is meant to float over the scheduler (spec 7.1).
            BoxWithConstraints(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize(),
            ) {
                val schedulerHeight = maxHeight
                Box(modifier = Modifier.fillMaxSize()) {
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
                        callbacks = schedulerCallbacks,
                        onZoomChange = onZoomChange,
                        onNewTask = onNewTask,
                        showEmptyState = interactive && !locked,
                        editable = !locked,
                    )
                    if (interactionBlocked) {
                        // The drawer scrim may not consume the first down event while its opening animation starts.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                        }
                                    }
                                },
                        )
                    }
                    NewTaskButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(LayoutConst.FAB_MARGIN_DP.dp),
                        visible = interactive && tasks.isNotEmpty() && !editing && !locked,
                        onClick = onNewTask,
                    )
                    if (!interactive) {
                        StatusOverlay(isError = isError, onRetry = onRetry)
                    }
                }
                if (editorLayout == EditorLayout.FLOATING_CARD) {
                    FloatingEditorCard(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .onGloballyPositioned { cardBounds = it.boundsInParent() },
                        visible = editing,
                        maxHeight = minOf(
                            schedulerHeight * LayoutConst.DIALOG_HEIGHT_FRACTION,
                            LayoutConst.DIALOG_MAX_HEIGHT_DP.dp,
                        ),
                        content = editorContent,
                    )
                }
            }
        }
        if (editorLayout == EditorLayout.FULL_PAGE) {
            FullPageEditor(visible = editing, content = editorContent)
        }
    }
}

/** The + button; hidden while editing so it never covers the editor (spec 7.3). */
@Composable
private fun NewTaskButton(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(LayoutConst.FAB_FADE_MILLIS)),
        exit = fadeOut(tween(LayoutConst.FAB_FADE_MILLIS)),
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.new_task),
            )
        }
    }
}

/**
 * The editor on wide windows: a card over the scheduler's bottom right, as tall as its content up to [maxHeight],
 * sliding in from the right. It covers the scheduler without squeezing it (spec 7.1).
 */
@Composable
private fun FloatingEditorCard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    maxHeight: Dp,
    content: @Composable (Modifier) -> Unit,
) {
    val slide = tween<IntOffset>(LayoutConst.EDITOR_PANEL_ANIMATION_MILLIS)
    val fade = tween<Float>(LayoutConst.EDITOR_PANEL_ANIMATION_MILLIS)
    AnimatedVisibility(
        visible = visible,
        // Clear of a side navigation bar or cutout on landscape phones, of the bottom navigation bar, and of the
        // keyboard, which lifts the card while typing.
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.displayCutout).union(WindowInsets.ime)
                    .only(WindowInsetsSides.End + WindowInsetsSides.Bottom),
            )
            // Flush with the right edge; only the bottom keeps a gap.
            .padding(bottom = LayoutConst.EDITOR_CARD_MARGIN_DP.dp),
        enter = slideInHorizontally(slide) { it } + fadeIn(fade),
        exit = slideOutHorizontally(slide) { it } + fadeOut(fade),
    ) {
        Surface(
            modifier = Modifier
                .width(LayoutConst.EDITOR_CARD_WIDTH_DP.dp)
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(LayoutConst.EDITOR_CARD_CORNER_DP.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = LayoutConst.EDITOR_CARD_ELEVATION_DP.dp,
        ) {
            // The card already sits above the navigation bar and keyboard, so the fields add no padding of their own.
            content(Modifier.consumeWindowInsets(WindowInsets.navigationBars.union(WindowInsets.ime)))
        }
    }
}

/** The editor on short, narrow windows: the whole screen from the start, no collapsed state (spec 7.1). */
@Composable
private fun FullPageEditor(
    visible: Boolean,
    content: @Composable (Modifier) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(LayoutConst.EDITOR_PANEL_ANIMATION_MILLIS)),
        exit = fadeOut(tween(LayoutConst.EDITOR_PANEL_ANIMATION_MILLIS)),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            content(
                Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            )
        }
    }
}

/** Background route used when there are no boards; Create board is presented over it by the app layer. */
@Composable
fun BlankBoardScreen(modifier: Modifier = Modifier) {
    val settings = TimelineSettings.DEFAULT
    val calendar = rememberCalendarContext()
    val state = rememberTimelineState(
        initialCenterTime = System.currentTimeMillis().toDouble(),
        initialUnit = settings.zoom.unit,
        initialDpPerUnit = settings.zoom.dpPerUnit,
        calendar = calendar,
    )
    val now = rememberNow()
    val todayDayOfMonth by remember(calendar) { derivedStateOf { calendar.localDate(now.value).dayOfMonth } }
    BoardContent(
        modifier = modifier,
        boardName = "",
        state = state,
        calendar = calendar,
        tasks = emptyList(),
        settings = settings,
        selectedTaskId = null,
        isLoading = false,
        isError = false,
        callbacks = TimelineCallbacks(),
        onToggleLeftPane = {},
        onRetry = {},
        todayDayOfMonth = todayDayOfMonth,
        onGoToToday = {},
        hasBoard = false,
    )
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
    Box(
        modifier = Modifier
            .topBarAction(onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LayoutConst.TODAY_BOX_SIZE_DP.dp)
                .border(
                    LayoutConst.TODAY_BOX_BORDER_DP.dp,
                    color,
                    RoundedCornerShape(LayoutConst.TODAY_BOX_CORNER_DP.dp),
                ),
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

/** The gap between the top-right buttons; larger on tablets. */
@Composable
private fun topBarActionGap(): Dp =
    (if (isTabletLayout()) LayoutConst.TOP_BAR_ACTION_GAP_TABLET_DP else LayoutConst.TOP_BAR_ACTION_GAP_PHONE_DP).dp

/** How far a top bar button is tappable beyond its content; more on tablets. */
@Composable
private fun topBarActionReach(): Dp = (
    if (isTabletLayout()) LayoutConst.TOP_BAR_ACTION_REACH_TABLET_DP else LayoutConst.TOP_BAR_ACTION_REACH_PHONE_DP
    ).dp

/** A top-right button: tappable a little beyond its content, with a circular ripple there. */
@Composable
private fun Modifier.topBarAction(onClick: () -> Unit): Modifier = clip(CircleShape)
    .clickable(role = Role.Button, onClick = onClick)
    .padding(topBarActionReach())

/**
 * The lock beside the board name (spec 9): a gray open lock, or a filled one in the primary color when locked. No
 * ripple, since the icon change is the feedback; tappable as far as the top-right buttons.
 */
@Composable
private fun LockIcon(
    modifier: Modifier = Modifier,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(if (locked) R.drawable.ic_lock else R.drawable.ic_lock_open),
        contentDescription = stringResource(if (locked) R.string.unlock_board else R.string.lock_board),
        modifier = modifier
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .padding(topBarActionReach())
            .size(LayoutConst.LOCK_ICON_SIZE_DP.dp),
        tint = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The ⋮ menu at the far right holds Settings and the board's less frequent Rename and Delete actions.
 */
@Composable
private fun OverflowMenu(
    onSettingsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.more_options),
            modifier = Modifier.topBarAction { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                onClick = {
                    expanded = false
                    onRenameClick()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
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
    sheet: TaskSheetState? = null,
    locked: Boolean = false,
) {
    PreviewSurface {
        BoardContent(
            boardName = if (isLoading || isError) "" else "Sample Project",
            locked = locked,
            showNavigation = true,
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
            sheet = sheet,
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
private fun BoardLockedPreview() {
    BoardPreview(tasks = FakeTimelineData.tasks(previewNow, previewCalendar), locked = true)
}

@ThemePreviews
@Composable
private fun BoardLockedEmptyPreview() {
    BoardPreview(tasks = emptyList(), locked = true)
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

@ThemePreviews
@Composable
private fun BoardEditingPreview() {
    BoardPreview(
        tasks = FakeTimelineData.tasks(previewNow, previewCalendar),
        selectedTaskId = 1L,
        sheet = TaskSheetState(
            title = "Kickoff meeting",
            notes = "Agenda: goals, roles, timeline",
            start = SheetMoment(LocalDate.of(2026, 10, 30), LocalTime.of(9, 0)),
            end = SheetMoment(LocalDate.of(2026, 10, 30), LocalTime.of(10, 30)),
            isAllDay = false,
            timeError = null,
            colorIndex = 0,
            canSave = true,
            isSaving = false,
            saveFailed = false,
        ),
    )
}
