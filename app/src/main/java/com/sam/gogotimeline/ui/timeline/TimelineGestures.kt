package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.gesture.DragDirection
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.gesture.HitTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** What each gesture means is decided by the scheduler; this file only recognizes which gesture happened. */
internal interface TimelineGestureHandler {
    fun hitTest(position: Offset): HitTarget

    /** Anywhere in the selected task's row can be long-pressed. */
    fun canLongPress(target: HitTarget): Boolean

    fun canMoveTime(target: HitTarget): Boolean

    fun onLongPress(target: HitTarget)

    fun onLongPressEnd()

    fun onTap(target: HitTarget)

    fun onDragStart(
        target: HitTarget,
        kind: DragKind,
        downPosition: Offset,
    )

    fun onDrag(position: Offset)

    fun onDragEnd()

    fun onDragCancel()

    fun onZoomEnd()

    fun onVerticalScrollSettled()
}

/**
 * All timeline gestures in one place so they never compete (spec 13.2 C).
 * - Handles drag right away, and so does a horizontal drag on the selected bar (moves the time).
 * - Any task row reorders after a long press (system timeout, no movement past touch slop) and a vertical drag;
 *   moving past slop first cancels the long press. Anything else scrolls in the locked direction and flings.
 * - Two fingers zoom and pan, except while dragging a task. A remaining single finger after a pinch does
 *   nothing until all fingers lift.
 * - Releasing without moving past slop and without a long press is a tap.
 */
internal fun Modifier.timelineGestures(
    state: TimelineState,
    handler: State<TimelineGestureHandler>,
): Modifier = pointerInput(state) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            launch { state.stopAnimation() }
            val callbacks = handler.value
            val target = callbacks.hitTest(down.position)
            val isHandle = target is HitTarget.StartHandle || target is HitTarget.EndHandle
            val canLongPress = callbacks.canLongPress(target)
            val canMoveTime = callbacks.canMoveTime(target)
            val longPressDeadline =
                down.uptimeMillis + GestureMath.longPressTimeout(viewConfiguration.longPressTimeoutMillis)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var lastUptime = down.uptimeMillis
            var longPressed = false
            var dragging = false
            var ignoreRest = false
            var pinched = false
            var cancelled = false
            var direction: DragDirection? = null
            var totalMove = Offset.Zero

            try {
                while (true) {
                    val waitingForLongPress = canLongPress && !longPressed && direction == null && !pinched
                    val event = if (waitingForLongPress) {
                        withTimeoutOrNull(longPressDeadline - lastUptime) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }
                    if (event == null) {
                        longPressed = true
                        callbacks.onLongPress(target)
                        continue
                    }
                    val pressedCount = event.changes.count { it.pressed }
                    if (pressedCount == 0) {
                        cancelled = wasCancelled(event)
                        break
                    }
                    if (pressedCount >= 2) {
                        // A second finger is ignored while a task is being dragged.
                        if (!dragging) {
                            pinched = true
                            val pan = event.calculatePan()
                            state.scrollBy(pan.x, pan.y)
                            state.zoomBy(event.calculateZoom(), event.calculateCentroid().x - state.axisLeftPx)
                        }
                        event.changes.forEach { it.consume() }
                        continue
                    }
                    if (pinched) continue

                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    lastUptime = change.uptimeMillis
                    tracker.addPosition(change.uptimeMillis, change.position)
                    if (dragging) {
                        callbacks.onDrag(change.position)
                        change.consume()
                        continue
                    }
                    if (ignoreRest) continue
                    val delta = change.positionChange()
                    val locked = direction ?: run {
                        totalMove += delta
                        GestureMath.lockDirection(totalMove.x, totalMove.y, viewConfiguration.touchSlop)
                    } ?: continue
                    if (direction == null) {
                        direction = locked
                        val kind = dragKind(target, longPressed, canMoveTime, locked)
                        if (kind != null) {
                            dragging = true
                            callbacks.onDragStart(target, kind, down.position)
                            callbacks.onDrag(change.position)
                            change.consume()
                            continue
                        }
                        if (longPressed) {
                            // After a long press, a horizontal drag off the selected bar does nothing.
                            ignoreRest = true
                            continue
                        }
                    }
                    when (locked) {
                        DragDirection.HORIZONTAL -> state.scrollBy(delta.x, 0f)
                        DragDirection.VERTICAL -> state.scrollBy(0f, delta.y)
                    }
                    change.consume()
                }
            } catch (cancelled: CancellationException) {
                if (dragging) callbacks.onDragCancel()
                throw cancelled
            } finally {
                if (longPressed) callbacks.onLongPressEnd()
            }

            val locked = direction
            when {
                // The system took the touch (e.g. an edge back swipe): not a tap, fling, or drop.
                cancelled -> if (dragging) callbacks.onDragCancel()

                dragging -> callbacks.onDragEnd()

                pinched -> callbacks.onZoomEnd()

                locked == null && !longPressed -> callbacks.onTap(target)

                locked != null && !longPressed && !isHandle -> {
                    val velocity = tracker.calculateVelocity()
                    val flingVelocity = when (locked) {
                        DragDirection.HORIZONTAL -> Velocity(velocity.x, 0f)
                        DragDirection.VERTICAL -> Velocity(0f, velocity.y)
                    }
                    launch {
                        state.fling(flingVelocity)
                        // Not reached when a new touch interrupts the fling; that gesture reports instead.
                        if (locked == DragDirection.VERTICAL) callbacks.onVerticalScrollSettled()
                    }
                }
            }
        }
    }
}

/**
 * True when all fingers "lifted" because the system took the touch (e.g. an edge back swipe) rather than a real
 * release: Compose reports a cancel as a release that keeps the previous event's time, while a real release is
 * always later than the event before it.
 */
internal fun wasCancelled(event: PointerEvent): Boolean =
    event.changes.isNotEmpty() && event.changes.all { !it.pressed && it.uptimeMillis == it.previousUptimeMillis }

/** What a drag does once its direction locks; null means scroll (or nothing after a long press). */
internal fun dragKind(
    target: HitTarget,
    longPressed: Boolean,
    canMoveTime: Boolean,
    direction: DragDirection,
): DragKind? = when {
    target is HitTarget.StartHandle -> DragKind.RESIZE_START
    target is HitTarget.EndHandle -> DragKind.RESIZE_END
    direction == DragDirection.HORIZONTAL && canMoveTime -> DragKind.MOVE
    direction == DragDirection.VERTICAL && longPressed -> DragKind.REORDER
    else -> null
}
