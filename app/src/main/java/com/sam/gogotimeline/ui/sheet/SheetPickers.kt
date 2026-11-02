package com.sam.gogotimeline.ui.sheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sam.gogotimeline.R
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.SheetTimeEdit
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.ui.preview.PreviewSurface
import com.sam.gogotimeline.ui.preview.ThemePreviews
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Material 3's date picker, limited to the scroll range, in the same style as the Save / Discard dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetDatePickerDialog(
    initial: LocalDate,
    boundary: Boundary,
    time: LocalTime?,
    calendar: CalendarContext,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The picker works in UTC midnights, so dates convert through UTC to stay the same day in every zone.
    val selectableDates = remember(boundary, time, calendar) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return isDateWithinTimeline(date, boundary, time, calendar)
            }
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = TimeConst.MIN_YEAR..TimeConst.MAX_YEAR,
        selectableDates = selectableDates,
    )
    val colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = { CancelButton(onDismiss) },
        shape = RoundedCornerShape(LayoutConst.SAVE_DIALOG_CORNER_DP.dp),
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}

internal fun isDateWithinTimeline(
    date: LocalDate,
    boundary: Boundary,
    time: LocalTime?,
    calendar: CalendarContext,
): Boolean {
    val value = when {
        time != null -> date.atTime(time).atZone(calendar.zone).toInstant().toEpochMilli()
        boundary == Boundary.START -> calendar.startOfDay(date)
        else -> calendar.startOfDay(date.plusDays(1))
    }
    return when (boundary) {
        Boundary.START -> value in TimeLimits.MIN until TimeLimits.MAX
        Boundary.END -> value > TimeLimits.MIN && value <= TimeLimits.MAX
    }
}

/**
 * The sheet's time picker (spec 7): hours on the left and minutes on the right, each stepped by arrows that wrap and
 * repeat while held. Material's time picker cannot limit minutes to 15-minute steps, hence a custom one.
 */
@Composable
internal fun SheetTimePickerDialog(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    // Wider than the platform default: each half needs room for arrow + two digits + arrow, or the number is
    // pushed off center.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TimePickerContent(
            modifier = Modifier.padding(horizontal = LayoutConst.TIME_PICKER_SIDE_MARGIN_DP.dp),
            initial = initial,
            onPick = onPick,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TimePickerContent(
    modifier: Modifier = Modifier,
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    // The numbers have a fixed size (not the system font), so arrow + digits + arrow always fit half the width.
    val numberSize = with(LocalDensity.current) { LayoutConst.TIME_PICKER_NUMBER_DP.dp.toSp() }
    val numberStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = numberSize, lineHeight = numberSize)
    Surface(
        modifier = modifier
            .widthIn(max = LayoutConst.TIME_PICKER_MAX_WIDTH_DP.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(LayoutConst.SAVE_DIALOG_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LayoutConst.SAVE_DIALOG_ELEVATION_DP.dp,
    ) {
        Column(
            modifier = Modifier.padding(LayoutConst.SAVE_DIALOG_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(LayoutConst.SHEET_SPACING_DP.dp),
        ) {
            // Hours and minutes split the width evenly around a fixed colon column, so each title sits centered
            // over its stepper.
            val colonWidth = LayoutConst.TIME_PICKER_COLON_WIDTH_DP.dp
            Row(modifier = Modifier.fillMaxWidth()) {
                UnitTitle(modifier = Modifier.weight(1f), text = stringResource(R.string.hour))
                Spacer(modifier = Modifier.width(colonWidth))
                UnitTitle(modifier = Modifier.weight(1f), text = stringResource(R.string.minute))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Stepper(
                    modifier = Modifier.weight(1f),
                    value = hour,
                    numberStyle = numberStyle,
                    previousLabel = stringResource(R.string.previous_hour),
                    nextLabel = stringResource(R.string.next_hour),
                    onStep = { hour = SheetTimeEdit.stepHour(hour, it) },
                )
                Text(
                    text = ":",
                    modifier = Modifier.width(colonWidth),
                    style = numberStyle,
                    textAlign = TextAlign.Center,
                )
                Stepper(
                    modifier = Modifier.weight(1f),
                    value = minute,
                    numberStyle = numberStyle,
                    previousLabel = stringResource(R.string.previous_minutes),
                    nextLabel = stringResource(R.string.next_minutes),
                    onStep = { minute = SheetTimeEdit.stepMinute(minute, it) },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CancelButton(onDismiss)
                TextButton(
                    onClick = {
                        onPick(LocalTime.of(hour, minute))
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}

/** `Hour` or `Minute` above its stepper, so the two numbers are not confused. */
@Composable
private fun UnitTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** `‹ 13 ›`: a two-digit value between a decrease and an increase arrow. */
@Composable
private fun Stepper(
    modifier: Modifier = Modifier,
    value: Int,
    numberStyle: TextStyle,
    previousLabel: String,
    nextLabel: String,
    onStep: (Int) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(iconRes = R.drawable.ic_chevron_left, label = previousLabel, onStep = { onStep(-1) })
        Text(text = "%02d".format(value), style = numberStyle)
        StepButton(iconRes = R.drawable.ic_chevron_right, label = nextLabel, onStep = { onStep(1) })
    }
}

/**
 * A gray round arrow like the sheet's ×, with a 48dp touch target. A tap steps once; holding repeats after a short
 * delay until the finger lifts. Screen readers activate it like a button.
 */
@Composable
private fun StepButton(
    @DrawableRes iconRes: Int,
    label: String,
    onStep: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val currentOnStep by rememberUpdatedState(onStep)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    currentOnStep()
                    true
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        currentOnStep()
                        val repeating = launch {
                            delay(LayoutConst.TIME_PICKER_REPEAT_DELAY_MILLIS)
                            while (true) {
                                currentOnStep()
                                delay(LayoutConst.TIME_PICKER_REPEAT_MILLIS)
                            }
                        }
                        waitForUpOrCancellation()
                        repeating.cancel()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LayoutConst.SHEET_BUTTON_SIZE_DP.dp)
                .background(colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(LayoutConst.SHEET_BUTTON_ICON_DP.dp),
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CancelButton(onDismiss: () -> Unit) {
    TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(stringResource(R.string.cancel))
    }
}

// Previews show the content without the Dialog window, which previews cannot render inline.
@OptIn(ExperimentalMaterial3Api::class)
@ThemePreviews
@Composable
private fun DatePickerPreview() {
    PreviewSurface {
        val day = LocalDate.of(2026, 10, 31)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@ThemePreviews
@Composable
private fun TimePickerPreview() {
    PreviewSurface {
        TimePickerContent(initial = LocalTime.of(13, 30), onPick = {}, onDismiss = {})
    }
}
