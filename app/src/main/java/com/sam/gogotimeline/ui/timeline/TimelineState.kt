package com.sam.gogotimeline.ui.timeline

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.TimeConst
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.time.CalendarContext
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.TimeScale
import com.sam.gogotimeline.core.time.TimeUnit
import com.sam.gogotimeline.core.time.Viewport
import com.sam.gogotimeline.core.time.ZoomLevel
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * What the timeline currently shows. Read only during layout and drawing, so scrolling re-places
 * and redraws without recomposing. All math is delegated to core/time and core/gesture.
 */
@Stable
class TimelineState(
    initialCenterTime: Double,
    initialUnit: TimeUnit,
    initialDpPerUnit: Float,
    initialScrollY: Float = 0f,
    calendar: CalendarContext,
) {
    var centerTime by mutableDoubleStateOf(initialCenterTime)
        private set
    var unit by mutableStateOf(initialUnit)
        private set
    var dpPerUnit by mutableFloatStateOf(initialDpPerUnit)
        private set
    var scrollY by mutableFloatStateOf(initialScrollY)
        private set

    var calendar by mutableStateOf(calendar)
        internal set
    var widthMultiplier by mutableFloatStateOf(TimeConst.WIDTH_MULTIPLIER_DEFAULT)
        internal set
    var density by mutableFloatStateOf(1f)
        private set
    var axisLeftPx by mutableFloatStateOf(0f)
        private set
    var viewportWidthPx by mutableFloatStateOf(0f)
        private set
    var viewportHeightPx by mutableFloatStateOf(0f)
        private set
    private var rowCount by mutableIntStateOf(0)
    private var rowHeightPx by mutableFloatStateOf(0f)

    // Only one animation (fling or unit-button zoom) changes the view at a time; a touch cancels it.
    private val mutatorMutex = MutatorMutex()

    val viewport: Viewport get() = Viewport(centerTime, unit, dpPerUnit)

    val zoomLevel: ZoomLevel get() = ZoomLevel(unit, dpPerUnit)

    fun scale(): TimeScale = TimeScale(viewport, calendar, density, viewportWidthPx)

    fun timeToX(time: Double): Float = scale().timeToX(time)

    fun xToTime(x: Float): Double = scale().xToTime(x)

    fun visibleTimeRange(): TimeRange = scale().visibleTimeRange()

    fun visibleRowRange(): IntRange = GestureMath.visibleRowRange(
        scrollY,
        viewportHeightPx,
        rowHeightPx,
        rowCount,
        LayoutConst.ROW_BUFFER,
    )

    /** [dx] and [dy] are finger movement in px; content follows the finger. Returns the movement used. */
    fun scrollBy(
        dx: Float,
        dy: Float,
    ): Offset {
        val oldCenterTime = centerTime
        val oldScrollY = scrollY
        if (dx != 0f) centerTime = scale().panBy(dx).centerTime
        if (dy != 0f) scrollY = clampScrollY(scrollY - dy)
        return Offset(if (centerTime != oldCenterTime) dx else 0f, oldScrollY - scrollY)
    }

    /** Zooms around [focusX] (px from the time axis' left edge); the unit may switch automatically. */
    fun zoomBy(
        factor: Float,
        focusX: Float,
    ) {
        val zoomed = scale().zoomBy(factor, focusX, widthMultiplier)
        centerTime = zoomed.centerTime
        unit = zoomed.unit
        dpPerUnit = zoomed.dpPerUnit
    }

    /** Decays [velocity] (px/s) and stops at a limit or when a touch calls [stopAnimation]. */
    suspend fun fling(velocity: Velocity) = mutatorMutex.mutate {
        coroutineScope {
            if (velocity.x != 0f) launch { decay(velocity.x) { scrollBy(it, 0f).x } }
            if (velocity.y != 0f) launch { decay(velocity.y) { scrollBy(0f, it).y } }
        }
    }

    /**
     * Smoothly zooms to [target] around the center. The animation runs in the coarser of the two units
     * so the header never builds thousands of fine ticks mid-way, and on a log scale so it looks even.
     */
    suspend fun animateZoomTo(target: ZoomLevel) = mutatorMutex.mutate {
        val animationUnit = if (target.unit.ordinal > unit.ordinal) target.unit else unit
        val from = scale().convertTo(animationUnit).dpPerUnit
        val to = TimeScale(Viewport(centerTime, target.unit, target.dpPerUnit), calendar, density, viewportWidthPx)
            .convertTo(animationUnit)
            .dpPerUnit
        unit = animationUnit
        dpPerUnit = from
        animate(0f, 1f, animationSpec = tween(LayoutConst.ZOOM_ANIMATION_MILLIS)) { progress, _ ->
            dpPerUnit = from * (to / from).pow(progress)
        }
        unit = target.unit
        dpPerUnit = target.dpPerUnit
    }

    /** Smoothly scrolls so [time] is centered, keeping unit and zoom. */
    suspend fun animateCenterTo(time: Double) = animateTimeToX(time, viewportWidthPx / 2)

    /**
     * Smoothly scrolls so [time] lands at [x] px from the time axis' left edge, keeping unit and zoom. Moves in
     * unit coordinates so months of different lengths scroll at an even speed; takes the same time however
     * far away [time] is, and stays within the scroll limits.
     */
    suspend fun animateTimeToX(
        time: Double,
        x: Float,
    ) = mutatorMutex.mutate {
        val target = TimeLimits.clamp(time)
        val axis = calendar.axis(unit)
        val offset = (x - viewportWidthPx / 2) / (dpPerUnit.toDouble() * density)
        val from = axis.position(centerTime)
        val to = axis.position(target) - offset
        animate(0f, 1f, animationSpec = tween(LayoutConst.ZOOM_ANIMATION_MILLIS)) { progress, _ ->
            centerTime = TimeLimits.clamp(axis.timeAt(from + (to - from) * progress))
        }
        // Centering lands exactly on the target, without a tiny rounding error from the round trip.
        centerTime = TimeLimits.clamp(if (offset == 0.0) target else axis.timeAt(to))
    }

    /** Called on touch-down: cancels a running fling or zoom animation. */
    suspend fun stopAnimation() {
        mutatorMutex.mutate(MutatePriority.UserInput) {}
    }

    internal fun updateLayout(
        density: Float,
        axisLeftPx: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        rowCount: Int,
        rowHeightPx: Float,
    ) {
        this.density = density
        this.axisLeftPx = axisLeftPx
        this.viewportWidthPx = viewportWidthPx
        this.viewportHeightPx = viewportHeightPx
        this.rowCount = rowCount
        this.rowHeightPx = rowHeightPx
        scrollY = clampScrollY(scrollY)
    }

    private fun clampScrollY(value: Float): Float =
        GestureMath.clampScrollY(value, rowCount, rowHeightPx, viewportHeightPx)

    private suspend fun decay(
        velocity: Float,
        scroll: (Float) -> Float,
    ) {
        var last = 0f
        AnimationState(0f, velocity).animateDecay(exponentialDecay()) {
            val delta = value - last
            last = value
            if (delta != 0f && scroll(delta) == 0f) cancelAnimation()
        }
    }

    companion object {
        /** Keeps center time, unit, width, and vertical scroll across rotation; the unit is saved by name. */
        fun saver(calendar: CalendarContext): Saver<TimelineState, Any> = listSaver(
            save = { listOf(it.centerTime, it.unit.name, it.dpPerUnit, it.scrollY) },
            restore = {
                TimelineState(
                    initialCenterTime = it[0] as Double,
                    initialUnit = TimeUnit.valueOf(it[1] as String),
                    initialDpPerUnit = it[2] as Float,
                    initialScrollY = it[3] as Float,
                    calendar = calendar,
                )
            },
        )
    }
}

@Composable
fun rememberTimelineState(
    initialCenterTime: Double,
    initialUnit: TimeUnit,
    initialDpPerUnit: Float,
    calendar: CalendarContext,
): TimelineState = rememberSaveable(saver = TimelineState.saver(calendar)) {
    TimelineState(
        initialCenterTime = initialCenterTime,
        initialUnit = initialUnit,
        initialDpPerUnit = initialDpPerUnit,
        calendar = calendar,
    )
}
