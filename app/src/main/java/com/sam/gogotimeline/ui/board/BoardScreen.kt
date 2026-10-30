package com.sam.gogotimeline.ui.board

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.ui.isTabletLayout
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
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
import kotlinx.coroutines.launch

/** The board screen. Until Room is wired up in step 4, it shows fake tasks and opens on "now" in Day mode. */
@Composable
fun BoardScreen(modifier: Modifier = Modifier) {
    val calendar = rememberCalendarContext()
    val state = rememberTimelineState(
        initialCenterTime = System.currentTimeMillis().toDouble(),
        initialUnit = TimeUnit.DAY,
        initialDpPerUnit = TimeUnit.DAY.standardWidthDp,
        calendar = calendar,
    )
    val tasks = remember(calendar) { FakeTimelineData.tasks(System.currentTimeMillis(), calendar) }
    var leftPaneExpanded by rememberSaveable { mutableStateOf(false) }
    val now = rememberNow()
    val todayDayOfMonth by remember(calendar) { derivedStateOf { calendar.localDate(now.value).dayOfMonth } }
    val scope = rememberCoroutineScope()
    BoardContent(
        modifier = modifier,
        boardName = stringResource(R.string.sample_board_name),
        state = state,
        calendar = calendar,
        tasks = tasks,
        leftPaneExpanded = leftPaneExpanded,
        onToggleLeftPane = { leftPaneExpanded = !leftPaneExpanded },
        todayDayOfMonth = todayDayOfMonth,
        onGoToToday = { scope.launch { state.animateCenterTo(System.currentTimeMillis().toDouble()) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardContent(
    modifier: Modifier = Modifier,
    boardName: String,
    state: TimelineState,
    calendar: CalendarContext,
    tasks: List<TimelineTask>,
    leftPaneExpanded: Boolean,
    onToggleLeftPane: () -> Unit,
    todayDayOfMonth: Int,
    onGoToToday: () -> Unit,
    onZoomChange: (ZoomLevel) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(boardName, style = MaterialTheme.typography.titleMedium) },
                actions = {
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
                },
            )
        },
    ) { innerPadding ->
        TimelineScheduler(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = state,
            calendar = calendar,
            tasks = tasks,
            leftPaneWidth = leftPaneWidth(isTabletLayout(), leftPaneExpanded),
            leftPaneExpanded = leftPaneExpanded,
            onToggleLeftPane = onToggleLeftPane,
            onZoomChange = onZoomChange,
        )
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

@ThemePreviews
@Composable
private fun BoardContentPreview() {
    val calendar = CalendarContext(ZoneId.of("Asia/Taipei"), DayOfWeek.SUNDAY)
    val now = LocalDateTime.of(2026, 10, 30, 12, 0).atZone(calendar.zone).toInstant().toEpochMilli()
    PreviewSurface {
        BoardContent(
            boardName = "Sample board",
            state = remember {
                TimelineState(
                    initialCenterTime = now.toDouble(),
                    initialUnit = TimeUnit.DAY,
                    initialDpPerUnit = TimeUnit.DAY.standardWidthDp,
                    calendar = calendar,
                )
            },
            calendar = calendar,
            tasks = FakeTimelineData.tasks(now, calendar),
            leftPaneExpanded = false,
            onToggleLeftPane = {},
            todayDayOfMonth = 30,
            onGoToToday = {},
        )
    }
}
