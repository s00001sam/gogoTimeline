package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.gesture.DragDirection
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.ZoomLevel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * All timeline gestures in one place so they never compete.
 * - One finger: locks to one direction past touch slop, scrolls along it, and flings on release.
 * - Two fingers: zoom around their midpoint and pan with them. Once a pinch starts, a remaining single
 *   finger does nothing; [onZoomChange] is reported once after all fingers lift.
 */
internal fun Modifier.timelineGestures(
    state: TimelineState,
    onZoomChange: State<(ZoomLevel) -> Unit>,
): Modifier = pointerInput(state) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            launch { state.stopAnimation() }
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var direction: DragDirection? = null
            var totalMove = Offset.Zero
            var pinched = false

            while (true) {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount == 0) break
                if (pressedCount >= 2) {
                    pinched = true
                    val pan = event.calculatePan()
                    state.scrollBy(pan.x, pan.y)
                    state.zoomBy(event.calculateZoom(), event.calculateCentroid().x - state.axisLeftPx)
                    event.changes.forEach { it.consume() }
                    continue
                }
                if (pinched) continue

                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPosition(change.uptimeMillis, change.position)
                val delta = change.positionChange()
                if (direction == null) {
                    totalMove += delta
                    direction = GestureMath.lockDirection(totalMove.x, totalMove.y, viewConfiguration.touchSlop)
                        ?: continue
                }
                when (direction) {
                    DragDirection.HORIZONTAL -> state.scrollBy(delta.x, 0f)
                    DragDirection.VERTICAL -> state.scrollBy(0f, delta.y)
                }
                change.consume()
            }

            if (pinched) {
                onZoomChange.value(state.zoomLevel)
                return@awaitEachGesture
            }
            val locked = direction ?: return@awaitEachGesture
            val velocity = tracker.calculateVelocity()
            val flingVelocity = when (locked) {
                DragDirection.HORIZONTAL -> Velocity(velocity.x, 0f)
                DragDirection.VERTICAL -> Velocity(0f, velocity.y)
            }
            launch { state.fling(flingVelocity) }
        }
    }
}
