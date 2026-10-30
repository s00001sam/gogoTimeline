package com.sam.gogotimeline.ui.board

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.ZoomLevel
import com.sam.gogotimeline.core.time.ZoomRules
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import com.sam.gogotimeline.ui.timeline.TimelineScheduler
import com.sam.gogotimeline.ui.timeline.TimelineState
import com.sam.gogotimeline.ui.timeline.rememberCalendarContext
import com.sam.gogotimeline.ui.timeline.rememberTimelineState
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/** The board screen. Until Room is wired up in step 4, it opens on "now" in Day mode. */
@Composable
fun BoardScreen(modifier: Modifier = Modifier) {
    val calendar = rememberCalendarContext()
    val state = rememberTimelineState(
        initialCenterTime = System.currentTimeMillis().toDouble(),
        initialUnit = TimeUnit.DAY,
        initialDpPerUnit = TimeUnit.DAY.standardWidthDp,
        calendar = calendar,
    )
    BoardContent(
        modifier = modifier,
        boardName = stringResource(R.string.sample_board_name),
        state = state,
        calendar = calendar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardContent(
    modifier: Modifier = Modifier,
    boardName: String,
    state: TimelineState,
    calendar: CalendarContext,
    onZoomChange: (ZoomLevel) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(boardName) },
                actions = {
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
            leftPaneWidth = (LayoutConst.NAME_COLUMN_PHONE_EXPANDED_DP + LayoutConst.TIME_COLUMN_WIDTH_DP).dp,
            onZoomChange = onZoomChange,
        )
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
    val center = LocalDateTime.of(2026, 10, 30, 12, 0).atZone(calendar.zone).toInstant().toEpochMilli()
    PreviewSurface {
        BoardContent(
            boardName = "Sample board",
            state = remember {
                TimelineState(
                    initialCenterTime = center.toDouble(),
                    initialUnit = TimeUnit.DAY,
                    initialDpPerUnit = TimeUnit.DAY.standardWidthDp,
                    calendar = calendar,
                )
            },
            calendar = calendar,
        )
    }
}
