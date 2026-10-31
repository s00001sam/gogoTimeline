package com.sam.gogotimeline.ui.timeline

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import com.sam.gogotimeline.core.gesture.HitTarget
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.core.time.local
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineInteractionTest {
    private val noon = TAIPEI.at("2026-10-30T12:00").toDouble()

    private fun task(
        id: Long,
        start: String,
        end: String,
    ) = TimelineTask(id, "Task $id", TAIPEI.at(start), TAIPEI.at(end), colorIndex = 0, isAllDay = false)

    private val visible = task(1, "2026-10-30T11:00", "2026-10-30T12:00")
    private val offScreenRight = task(2, "2026-10-31T09:00", "2026-10-31T10:00")
    private val partlyLeft = task(3, "2026-10-30T08:00", "2026-10-30T09:00")

    /** Records what the scheduler reports to its owner. */
    private class Recorder {
        val events = mutableListOf<String>()
        val callbacks = TimelineCallbacks(
            onTaskTap = { events += "tap $it" },
            onEmptyTap = { events += "empty" },
            onTaskTimeChange = { id, start, end -> events += "time $id ${TAIPEI.local(start)} ${TAIPEI.local(end)}" },
            onTaskMove = { id, to -> events += "move $id $to" },
        )
    }

    /** Runs animations instantly so a scroll-to-task finishes within advanceUntilIdle. */
    private class InstantFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }

    private fun TestScope.setUp(
        selectedTaskId: Long? = null,
        tasks: List<TimelineTask> = listOf(visible, offScreenRight, partlyLeft),
    ): Triple<TimelineInteraction, TimelineState, Recorder> {
        val state = TimelineState(noon, TimeUnit.HOUR, 64f, calendar = TAIPEI)
        state.updateLayout(DENSITY, AXIS_LEFT, AXIS_WIDTH, BODY_HEIGHT, tasks.size, ROW)
        val recorder = Recorder()
        val interaction = TimelineInteraction(state, CoroutineScope(coroutineContext + InstantFrameClock()))
        interaction.update(
            tasks = tasks,
            selectedTaskId = selectedTaskId,
            snap = SnapInterval(15),
            sizes = InteractionSizes(HEADER, ROW, 64f, 32f, 8f, 8f, MARGIN),
            callbacks = recorder.callbacks,
            labelText = { "label ${it.id}" },
            labelWidth = { it.length * 10f },
            haptic = {},
        )
        return Triple(interaction, state, recorder)
    }

    private fun rowY(row: Int): Float = HEADER + row * ROW + ROW / 2

    private fun axis(x: Float): Float = AXIS_LEFT + x

    @Test
    fun tapOnBarSelectsIt() = runTest {
        val (interaction, _, recorder) = setUp()
        interaction.onTap(interaction.hitTest(Offset(axis(450f), rowY(0))))
        assertEquals(listOf("tap 1"), recorder.events)
    }

    @Test
    fun tapBelowTheRowsEndsEditing() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        interaction.onTap(interaction.hitTest(Offset(axis(450f), HEADER + 3 * ROW + 10f)))
        assertEquals(listOf("empty"), recorder.events)
    }

    @Test
    fun anyRowCanBeLongPressed() = runTest {
        val (interaction, _, _) = setUp()
        assertTrue(interaction.canLongPress(HitTarget.RowBlank(1)))
        assertTrue(interaction.canLongPress(HitTarget.LeftPaneRow(2)))
        assertTrue(interaction.canLongPress(HitTarget.BarLabel(3)))
        assertFalse(interaction.canLongPress(HitTarget.StartHandle(1)))
        assertFalse(interaction.canLongPress(HitTarget.Empty))
    }

    @Test
    fun onlyTheSelectedBarMovesTime() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        assertTrue(interaction.canMoveTime(HitTarget.BarBody(1)))
        assertFalse(interaction.canMoveTime(HitTarget.BarBody(2)))
        assertFalse(interaction.canMoveTime(HitTarget.RowBlank(1)))
    }

    @Test
    fun unselectedTaskReordersWithoutBeingSelected() = runTest {
        val (interaction, _, recorder) = setUp()
        val down = Offset(axis(800f), rowY(1))
        interaction.onDragStart(HitTarget.RowBlank(2), DragKind.REORDER, down)
        interaction.onDrag(down + Offset(0f, ROW))
        interaction.onDragEnd()
        assertEquals(listOf("move 2 2"), recorder.events)
    }

    @Test
    fun moveReportsTheSnappedTime() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        val down = Offset(axis(400f), rowY(0))
        interaction.onDragStart(HitTarget.BarBody(1), DragKind.MOVE, down)
        interaction.onDrag(down + Offset(2 * HOUR_PX, 0f))
        interaction.onDragEnd()
        assertEquals(listOf("time 1 2026-10-30T13:00 2026-10-30T14:00"), recorder.events)
    }

    @Test
    fun blankInTheEditedRowKeepsEditing() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        interaction.onTap(HitTarget.RowBlank(1))
        assertTrue(recorder.events.isEmpty())
        interaction.onTap(HitTarget.RowBlank(2))
        assertEquals(listOf("empty"), recorder.events)
    }

    @Test
    fun dragBackToTheStartReportsNothing() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        val down = Offset(axis(400f), rowY(0))
        interaction.onDragStart(HitTarget.BarBody(1), DragKind.MOVE, down)
        interaction.onDrag(down + Offset(300f, 0f))
        interaction.onDrag(down)
        interaction.onDragEnd()
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun reorderReportsOnlyARealMove() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        val down = Offset(axis(800f), rowY(0))
        interaction.onDragStart(HitTarget.RowBlank(1), DragKind.REORDER, down)
        interaction.onDrag(down + Offset(0f, ROW))
        interaction.onDragEnd()
        interaction.onDragStart(HitTarget.RowBlank(1), DragKind.REORDER, down)
        interaction.onDrag(down + Offset(0f, 10f))
        interaction.onDragEnd()
        assertEquals(listOf("move 1 1"), recorder.events)
    }

    @Test
    fun leftPaneTapEditsVisibleTasksAndScrollsToOthers() = runTest {
        val (interaction, state, recorder) = setUp()
        interaction.onTap(HitTarget.LeftPaneRow(3))
        interaction.onTap(HitTarget.LeftPaneRow(2))
        advanceUntilIdle()
        assertEquals(listOf("tap 3"), recorder.events)
        assertEquals(MARGIN, state.timeToX(offScreenRight.start.toDouble()), 0.01f)
    }

    @Test
    fun releasedTimesStayUntilTheOwnerApplies() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        val down = Offset(axis(400f), rowY(0))
        interaction.onDragStart(HitTarget.BarBody(1), DragKind.MOVE, down)
        interaction.onDrag(down + Offset(2 * HOUR_PX, 0f))
        interaction.onDragEnd()
        val moved = TimeRange(TAIPEI.at("2026-10-30T13:00"), TAIPEI.at("2026-10-30T14:00"))
        assertEquals(moved, interaction.displayRange(visible))

        val applied = visible.copy(start = moved.start, end = moved.end)
        interaction.update(
            tasks = listOf(applied, offScreenRight, partlyLeft),
            selectedTaskId = 1,
            snap = SnapInterval(15),
            sizes = InteractionSizes(HEADER, ROW, 64f, 32f, 8f, 8f, MARGIN),
            callbacks = TimelineCallbacks(),
            labelText = { "" },
            labelWidth = { 0f },
            haptic = {},
        )
        interaction.clearReleasedIfApplied()
        assertEquals(TimeRange(visible.start, visible.end), interaction.displayRange(visible))
    }

    @Test
    fun cancelledDragReportsNothing() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        val down = Offset(axis(400f), rowY(0))
        interaction.onDragStart(HitTarget.BarBody(1), DragKind.MOVE, down)
        interaction.onDrag(down + Offset(2 * HOUR_PX, 0f))
        interaction.onDragCancel()
        assertNull(interaction.drag)
        assertTrue(recorder.events.isEmpty())
        assertEquals(TimeRange(visible.start, visible.end), interaction.displayRange(visible))
    }

    @Test
    fun pinnedLabelOfAnOffScreenTaskScrollsToIt() = runTest {
        val (interaction, state, recorder) = setUp()
        val target = interaction.hitTest(Offset(axis(20f), rowY(1)))
        assertEquals(HitTarget.BarLabel(2), target)
        interaction.onTap(target)
        advanceUntilIdle()
        assertTrue(recorder.events.isEmpty())
        assertEquals(MARGIN, state.timeToX(offScreenRight.start.toDouble()), 0.01f)
    }

    @Test
    fun editingThenTappingAnotherBarOnlyEndsEditing() = runTest {
        val (interaction, _, recorder) = setUp(selectedTaskId = 1)
        interaction.onTap(HitTarget.BarBody(3))
        assertEquals(listOf("empty"), recorder.events)
    }

    @Test
    fun editingThenTappingAnOffScreenNameDoesNotScroll() = runTest {
        val (interaction, state, recorder) = setUp(selectedTaskId = 1)
        interaction.onTap(HitTarget.LeftPaneRow(2))
        advanceUntilIdle()
        assertEquals(listOf("empty"), recorder.events)
        assertEquals(noon, state.centerTime, 0.0)
    }

    @Test
    fun selectedTasksOwnOffScreenNameStillScrolls() = runTest {
        val (interaction, state, recorder) = setUp(selectedTaskId = 2)
        interaction.onTap(HitTarget.LeftPaneRow(2))
        advanceUntilIdle()
        assertTrue(recorder.events.isEmpty())
        assertEquals(MARGIN, state.timeToX(offScreenRight.start.toDouble()), 0.01f)
    }

    @Test
    fun blankSpaceInARowEditsThatTask() = runTest {
        val (interaction, _, recorder) = setUp()
        interaction.onTap(HitTarget.RowBlank(1))
        assertEquals(listOf("tap 1"), recorder.events)
    }

    @Test
    fun blankSpaceOfAnOffScreenTaskOnlyScrolls() = runTest {
        val (interaction, state, recorder) = setUp()
        interaction.onTap(HitTarget.RowBlank(2))
        advanceUntilIdle()
        assertTrue(recorder.events.isEmpty())
        assertEquals(MARGIN, state.timeToX(offScreenRight.start.toDouble()), 0.01f)
    }

    private fun TimelineInteraction.receive(tasks: List<TimelineTask>) {
        update(
            tasks = tasks,
            selectedTaskId = 1,
            snap = SnapInterval(15),
            sizes = InteractionSizes(HEADER, ROW, 64f, 32f, 8f, 8f, MARGIN),
            callbacks = TimelineCallbacks(),
            labelText = { "" },
            labelWidth = { 0f },
            haptic = {},
        )
        clearReleasedIfApplied()
    }

    private fun TimelineInteraction.dropFirstRowOneDown() {
        val down = Offset(axis(800f), rowY(0))
        onDragStart(HitTarget.RowBlank(1), DragKind.REORDER, down)
        onDrag(down + Offset(0f, ROW))
        onDragEnd()
    }

    @Test
    fun droppedOrderShowsBeforeTheOwnerApplies() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.dropFirstRowOneDown()
        assertEquals(listOf(2L, 1L, 3L), interaction.rowTasks().map { it.id })
    }

    @Test
    fun droppedOrderIsDroppedOnceApplied() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.dropFirstRowOneDown()
        interaction.receive(listOf(offScreenRight, visible, partlyLeft))
        interaction.receive(listOf(visible, offScreenRight, partlyLeft))
        assertEquals(listOf(1L, 2L, 3L), interaction.rowTasks().map { it.id })
    }

    @Test
    fun droppedOrderOfADeletedTaskIsDropped() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.dropFirstRowOneDown()
        interaction.receive(listOf(offScreenRight, partlyLeft))
        assertEquals(listOf(2L, 3L), interaction.rowTasks().map { it.id })
    }

    @Test
    fun longPressLiftsTheRowUntilRelease() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.onLongPress(HitTarget.RowBlank(1))
        assertEquals(1L, interaction.liftedTaskId)
        interaction.onLongPressEnd()
        assertNull(interaction.liftedTaskId)
    }

    @Test
    fun rowStaysLiftedAfterTheDropUntilTheFingerLifts() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.onLongPress(HitTarget.RowBlank(1))
        interaction.dropFirstRowOneDown()
        assertEquals(1L, interaction.liftedTaskId)
        interaction.onLongPressEnd()
        assertNull(interaction.liftedTaskId)
    }

    @Test
    fun interruptedGestureLowersTheRow() = runTest {
        val (interaction, _, _) = setUp(selectedTaskId = 1)
        interaction.onLongPress(HitTarget.BarBody(1))
        interaction.onDragCancel()
        interaction.onLongPressEnd()
        assertNull(interaction.liftedTaskId)
    }

    private companion object {
        const val DENSITY = 2f
        const val AXIS_LEFT = 200f
        const val AXIS_WIDTH = 1_000f
        const val BODY_HEIGHT = 600f
        const val HEADER = 104f
        const val ROW = 128f
        const val MARGIN = 48f
        const val HOUR_PX = 128f
    }
}
