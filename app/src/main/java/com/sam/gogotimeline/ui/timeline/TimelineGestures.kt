package com.sam.gogotimeline.ui.timeline

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.gesture.DragDirection
import com.sam.gogotimeline.core.gesture.GestureMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * All timeline gestures in one place so they never compete. A drag locks to one direction once it
 * passes touch slop, scrolls only along it, and flings with that direction's velocity on release.
 */
internal fun Modifier.timelineGestures(state: TimelineState): Modifier = pointerInput(state) {
    coroutineScope {
        var flingJob: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            flingJob?.cancel()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var direction: DragDirection? = null
            var totalMove = Offset.Zero

            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
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

            val locked = direction ?: return@awaitEachGesture
            val velocity = tracker.calculateVelocity()
            val flingVelocity = when (locked) {
                DragDirection.HORIZONTAL -> Velocity(velocity.x, 0f)
                DragDirection.VERTICAL -> Velocity(0f, velocity.y)
            }
            flingJob = launch { state.fling(flingVelocity) }
        }
    }
}
