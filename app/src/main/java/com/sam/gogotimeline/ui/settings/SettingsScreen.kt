package com.sam.gogotimeline.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.RowHeight
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TaskTimeFormat
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeScale
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.Viewport
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.ui.board.ConfirmDialogContent
import com.sam.gogotimeline.ui.board.ConfirmDialogWindow
import com.sam.gogotimeline.ui.board.PrimaryDialogButton
import com.sam.gogotimeline.ui.board.SecondaryDialogButton
import com.sam.gogotimeline.ui.navigation.PageDialogFrame
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.snapIntervalLabel
import com.sam.gogotimeline.ui.theme.GogoTimelineTheme
import com.sam.gogotimeline.ui.timeline.LeftPaneRow
import com.sam.gogotimeline.ui.timeline.TaskBar
import com.sam.gogotimeline.ui.timeline.TickCache
import com.sam.gogotimeline.ui.timeline.TimelineTask
import com.sam.gogotimeline.ui.timeline.drawHeader
import com.sam.gogotimeline.ui.timeline.rememberCalendarContext
import com.sam.gogotimeline.ui.timeline.rememberTimelineTextStyles
import java.util.concurrent.TimeUnit as JavaTimeUnit

/**
 * Settings, saved as they change. On phones a full page with a back arrow; on tablets ([asDialog]) a centered
 * rounded dialog with × (spec 8). The board behind follows each change right away.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    asDialog: Boolean,
    onClose: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.settings_save_failed)
    LaunchedEffect(viewModel) { viewModel.saveFailed.collect { snackbarHostState.showSnackbar(failedMessage) } }
    val shown = state ?: return
    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        SettingsContent(
            modifier = contentModifier,
            state = shown,
            asDialog = asDialog,
            snackbarHostState = snackbarHostState,
            onClose = onClose,
            onSnapSelected = viewModel::onSnapSelected,
            onWidthDrag = viewModel::onWidthDrag,
            onWidthDragFinished = viewModel::onWidthDragFinished,
            onRowHeightDrag = viewModel::onRowHeightDrag,
            onRowHeightDragFinished = viewModel::onRowHeightDragFinished,
            onResetClick = viewModel::onResetClick,
        )
    }
    if (asDialog) PageDialogFrame(modifier, content) else content(modifier.fillMaxSize())
    if (shown.showResetDialog) {
        ResetDialog(onReset = viewModel::onResetConfirm, onCancel = viewModel::onResetDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    asDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSnapSelected: (SnapInterval) -> Unit,
    onWidthDrag: (Float) -> Unit,
    onWidthDragFinished: () -> Unit,
    onRowHeightDrag: (Float) -> Unit,
    onRowHeightDragFinished: () -> Unit,
    onResetClick: () -> Unit,
) {
    // Inside the dialog there are no system bars to avoid.
    val insets = if (asDialog) WindowInsets(0) else null
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(if (asDialog) R.drawable.ic_close else R.drawable.ic_arrow_back),
                            contentDescription = stringResource(if (asDialog) R.string.close else R.string.back),
                        )
                    }
                },
                windowInsets = insets ?: TopAppBarDefaults.windowInsets,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = insets ?: ScaffoldDefaults.contentWindowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LayoutConst.SETTINGS_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(LayoutConst.SETTINGS_SECTION_SPACING_DP.dp),
        ) {
            SnapSection(state = state, onSnapSelected = onSnapSelected)
            // The sample is the real header for the current unit, so the multiplier reads as "how wide one hour is".
            val widthMultiplier = ZoomRules.roundWidthMultiplier(state.shownWidthMultiplier)
            Column(verticalArrangement = Arrangement.spacedBy(LayoutConst.SETTINGS_SAMPLE_GAP_DP.dp)) {
                SliderSection(
                    title = stringResource(R.string.time_scale),
                    value = state.shownWidthMultiplier,
                    valueRange = TimeConst.WIDTH_MULTIPLIER_MIN..TimeConst.WIDTH_MULTIPLIER_MAX,
                    valueText = stringResource(R.string.multiplier_value, widthMultiplier),
                    onValueChange = onWidthDrag,
                    onValueChangeFinished = onWidthDragFinished,
                )
                ScaleSample(unit = state.currentUnit, dpPerUnit = state.currentUnit.standardWidthDp(widthMultiplier))
            }
            // Shown as a multiplier, never dp; the sample row previews the value it would save.
            val rowHeightMultiplier = RowHeight.roundMultiplier(state.shownRowHeightMultiplier)
            Column(verticalArrangement = Arrangement.spacedBy(LayoutConst.SETTINGS_SAMPLE_GAP_DP.dp)) {
                SliderSection(
                    title = stringResource(R.string.row_height),
                    value = state.shownRowHeightMultiplier,
                    valueRange = LayoutConst.ROW_HEIGHT_MULTIPLIER_MIN..LayoutConst.ROW_HEIGHT_MULTIPLIER_MAX,
                    valueText = stringResource(R.string.multiplier_value, rowHeightMultiplier),
                    onValueChange = onRowHeightDrag,
                    onValueChangeFinished = onRowHeightDragFinished,
                )
                SampleRow(rowHeightDp = RowHeight.toDp(rowHeightMultiplier))
            }
            // Kept at the bottom, away from the common settings, so it is hard to hit by mistake.
            OutlinedButton(
                onClick = onResetClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = LayoutConst.SETTINGS_RESET_EXTRA_GAP_DP.dp),
            ) {
                Text(stringResource(R.string.reset_to_defaults))
            }
        }
    }
}

/** `Snap interval (Hour)` over equal segments for the board's current unit only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapSection(
    state: SettingsUiState,
    onSnapSelected: (SnapInterval) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp)) {
        SectionTitle(stringResource(R.string.snap_interval_title, stringResource(state.currentUnit.labelRes())))
        val options = state.snapOptions
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == state.selectedSnap,
                    onClick = { onSnapSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    // The selected color already marks the choice; no check mark, so labels keep their room.
                    icon = {},
                    label = { Text(snapIntervalLabel(option)) },
                )
            }
        }
    }
}

/** A title above a slider with its value on the right; dragging is continuous, the saved value is rounded. */
@Composable
private fun SliderSection(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column {
        SectionTitle(title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier.weight(1f),
                valueRange = valueRange,
            )
            Text(
                text = valueText,
                modifier = Modifier
                    .padding(start = LayoutConst.SETTINGS_PADDING_DP.dp)
                    .widthIn(min = LayoutConst.SETTINGS_VALUE_MIN_WIDTH_DP.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

/** The scheduler's two-tier header around now, drawn at [dpPerUnit]; scheduler text keeps its fixed size. */
@Composable
private fun ScaleSample(
    unit: TimeUnit,
    dpPerUnit: Float,
) {
    val calendar = rememberCalendarContext()
    val styles = rememberTimelineTextStyles()
    val textMeasurer = rememberTextMeasurer()
    val tickCache = remember { TickCache() }
    val colorScheme = MaterialTheme.colorScheme
    val gridColor = GogoTimelineTheme.extendedColors.gridMajor
    val topStyle = styles.tickTop.copy(color = colorScheme.onSurface)
    val bottomStyle = styles.tickBottom.copy(color = colorScheme.onSurfaceVariant)
    val center = remember(calendar) { System.currentTimeMillis().toDouble() }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height((LayoutConst.HEADER_TOP_TIER_HEIGHT_DP + LayoutConst.HEADER_BOTTOM_TIER_HEIGHT_DP).dp)
            .clipToBounds()
            .background(colorScheme.background),
    ) {
        val scale = TimeScale(Viewport(center, unit, dpPerUnit), calendar, density, size.width)
        val ticks = tickCache.ticks(scale.visibleTimeRange(), unit, dpPerUnit, calendar)
        drawHeader(ticks, scale, textMeasurer, topStyle, bottomStyle, gridColor)
    }
}

/**
 * A row as the scheduler draws it, at the chosen height: a name in the left pane and a bar with its time label.
 * Scheduler text has a fixed size, like on the board.
 */
@Composable
private fun SampleRow(rowHeightDp: Float) {
    val calendar = rememberCalendarContext()
    val styles = rememberTimelineTextStyles()
    val textMeasurer = rememberTextMeasurer()
    val title = stringResource(R.string.sample_task)
    val task = remember(calendar, title) {
        val today = calendar.startOfDay(calendar.localDate(System.currentTimeMillis()))
        val start = today + JavaTimeUnit.HOURS.toMillis(SAMPLE_START_HOUR)
        val end = start + JavaTimeUnit.MINUTES.toMillis(SAMPLE_LENGTH_MINUTES)
        TimelineTask(id = 0L, title = title, start = start, end = end, colorIndex = SAMPLE_COLOR, isAllDay = false)
    }
    val barText = remember(task, calendar) {
        TaskTimeFormat.barText(
            TimeRange(task.start, task.end),
            isAllDay = false,
            now = task.start,
            calendar = calendar,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeightDp.dp)
            .background(MaterialTheme.colorScheme.background),
    ) {
        LeftPaneRow(
            modifier = Modifier
                .width(LayoutConst.NAME_COLUMN_PHONE_COLLAPSED_DP.dp)
                .height(rowHeightDp.dp),
            title = task.title,
            styles = styles,
            selected = false,
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = LayoutConst.SHEET_SPACING_DP.dp,
                    vertical = LayoutConst.BAR_VERTICAL_PADDING_DP.dp,
                ),
        ) {
            val barWidthPx = constraints.maxWidth.toFloat()
            TaskBar(
                modifier = Modifier.fillMaxSize(),
                task = task,
                text = barText,
                selected = false,
                textMeasurer = textMeasurer,
                style = styles.barText,
                barLeftPx = { 0f },
                axisWidthPx = { barWidthPx },
            )
        }
    }
}

/** Confirms resetting the settings page's values; same compact style as the other confirm dialogs. */
@Composable
private fun ResetDialog(
    onReset: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogWindow(isBusy = false, onCancel = onCancel) {
        ResetDialogContent(onReset = onReset, onCancel = onCancel)
    }
}

@Composable
private fun ResetDialogContent(
    onReset: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmDialogContent(
        title = stringResource(R.string.reset_settings_title),
        message = null,
        error = null,
    ) {
        SecondaryDialogButton(text = stringResource(R.string.cancel), enabled = true, onClick = onCancel)
        PrimaryDialogButton(text = stringResource(R.string.reset), enabled = true, isBusy = false, onClick = onReset)
    }
}

@StringRes
private fun TimeUnit.labelRes(): Int = when (this) {
    TimeUnit.HOUR -> R.string.unit_hour
    TimeUnit.DAY -> R.string.unit_day
    TimeUnit.WEEK -> R.string.unit_week
    TimeUnit.MONTH -> R.string.unit_month
}

private const val SAMPLE_START_HOUR = 9L
private const val SAMPLE_LENGTH_MINUTES = 90L
private const val SAMPLE_COLOR = 4

private val previewState = SettingsUiState(
    currentUnit = TimeUnit.HOUR,
    selectedSnap = SnapInterval(15),
    widthMultiplier = 1f,
    rowHeightDp = 64f,
)

@Composable
private fun SettingsPreview(
    state: SettingsUiState,
    asDialog: Boolean = false,
) {
    PreviewSurface {
        val content: @Composable (Modifier) -> Unit = { modifier ->
            SettingsContent(
                modifier = modifier,
                state = state,
                asDialog = asDialog,
                snackbarHostState = remember { SnackbarHostState() },
                onClose = {},
                onSnapSelected = {},
                onWidthDrag = {},
                onWidthDragFinished = {},
                onRowHeightDrag = {},
                onRowHeightDragFinished = {},
                onResetClick = {},
            )
        }
        if (asDialog) PageDialogFrame(content = content) else content(Modifier.fillMaxSize())
    }
}

@ThemePreviews
@Composable
private fun SettingsHourPreview() = SettingsPreview(previewState)

@ThemePreviews
@Composable
private fun SettingsDayDraggingPreview() = SettingsPreview(
    previewState.copy(
        currentUnit = TimeUnit.DAY,
        selectedSnap = SnapInterval(180),
        pendingWidthMultiplier = 1.37f,
        pendingRowHeightMultiplier = 0.8f,
    ),
)

@ThemePreviews
@Composable
private fun SettingsDialogPreview() = SettingsPreview(previewState.copy(widthMultiplier = 2f), asDialog = true)

// Previews show the content without the Dialog window, which previews cannot render inline.
@ThemePreviews
@Composable
private fun ResetDialogPreview() {
    PreviewSurface { ResetDialogContent(onReset = {}, onCancel = {}) }
}
