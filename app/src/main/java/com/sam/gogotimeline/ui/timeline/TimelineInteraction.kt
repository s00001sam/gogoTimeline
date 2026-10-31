package com.sam.gogotimeline.ui.timeline

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import com.sam.gogotimeline.core.LayoutConst
import com.sam.gogotimeline.core.gesture.GestureMath
import com.sam.gogotimeline.core.gesture.HitLayout
import com.sam.gogotimeline.core.gesture.HitTarget
import com.sam.gogotimeline.core.gesture.HitTest
import com.sam.gogotimeline.core.gesture.RowHitArea
import com.sam.gogotimeline.core.time.SnapInterval
import com.sam.gogotimeline.core.time.TimeGrid
import com.sam.gogotimeline.core.time.TimeRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Callbacks the scheduler reports to its owner. The scheduler never changes task data itself. */
data class TimelineCallbacks(
    val onTaskTap: (taskId: Long) -> Unit = {},
    val onEmptyTap: () -> Unit = {},
    val onTaskTimeChange: (taskId: Long, start: Long, end: Long) -> Unit = { _, _, _ -> },
    val onTaskMove: (taskId: Long, toIndex: Int) -> Unit = { _, _ -> },
    val onZoomEnd: () -> Unit = {},
    val onVerticalScrollSettled: (topRowIndex: Int) -> Unit = {},
)

/** Sizes in px that the gestures need, measured by the scheduler. */
data class InteractionSizes(
    val headerHeight: Float,
    val rowHeight: Float,
    val handleOutside: Float,
    val handleInside: Float,
    val labelGap: Float,
    val labelPaddingX: Float,
    val scrollToTaskMargin: Float,
)

/**
 * What each gesture means for the scheduler, plus the state of a drag in progress. Inputs are refreshed by
 * [update] on every composition; the drag state is snapshot state so the bars follow the finger.
 */
@Stable
internal class TimelineInteraction(
    private val state: TimelineState,
    private val scope: CoroutineScope,
) : TimelineGestureHandler {
    private var tasks: List<TimelineTask> = emptyList()
    private var selectedTaskId: Long? = null
    private var snap: SnapInterval = SnapInterval.default(state.unit)
    private var sizes = InteractionSizes(0f, 1f, 0f, 0f, 0f, 0f, 0f)
    private var callbacks = TimelineCallbacks()
    private var labelText: (TimelineTask) -> String = { "" }
    private var labelWidth: (String) -> Float = { 0f }
    private var haptic: () -> Unit = {}

    var drag by mutableStateOf<DragSession?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set

    /** The row lifted by a long press, from the moment it succeeds until the finger lifts. */
    var liftedTaskId by mutableStateOf<Long?>(null)
        private set

    // Kept after release until the new times come back in the tasks, so the bar never flashes back for a frame.
    private var released by mutableStateOf<Pair<Long, TimeRange>?>(null)

    // The same for a reorder: task id to its new row, kept until the new order comes back.
    private var releasedMove by mutableStateOf<Pair<Long, Int>?>(null)

    fun update(
        tasks: List<TimelineTask>,
        selectedTaskId: Long?,
        snap: SnapInterval,
        sizes: InteractionSizes,
        callbacks: TimelineCallbacks,
        labelText: (TimelineTask) -> String,
        labelWidth: (String) -> Float,
        haptic: () -> Unit,
    ) {
        this.tasks = tasks
        this.selectedTaskId = selectedTaskId
        this.snap = snap
        this.sizes = sizes
        this.callbacks = callbacks
        this.labelText = labelText
        this.labelWidth = labelWidth
        this.haptic = haptic
    }

    /** Call after new tasks arrive: drops the kept times or order once the owner has them (or the task is gone). */
    fun clearReleasedIfApplied() {
        released?.let { (id, range) ->
            val task = tasks.firstOrNull { it.id == id }
            if (task == null || (task.start == range.start && task.end == range.end)) released = null
        }
        releasedMove?.let { (id, to) ->
            val index = tasks.indexOfFirst { it.id == id }
            if (index < 0 || index == to) releasedMove = null
        }
    }

    /** Row order to draw and hit-test: the dropped order until the owner's tasks catch up. */
    fun rowTasks(): List<TimelineTask> {
        val (id, to) = releasedMove ?: return tasks
        val from = tasks.indexOfFirst { it.id == id }
        return if (from < 0) tasks else GestureMath.moveItem(tasks, from, to)
    }

    fun grid(): TimeGrid = state.calendar.grid(snap)

    /** Preview times of the task being moved or resized, or null when not dragging times. */
    fun previewRange(): TimeRange? {
        val session = drag ?: return null
        if (session.kind == DragKind.REORDER) return null
        return session.previewRange(state.xToTime(pointer.x - state.axisLeftPx), grid())
    }

    fun reorderTarget(): Int? {
        val session = drag?.takeIf { it.kind == DragKind.REORDER } ?: return null
        return session.targetIndex(contentY(pointer.y), sizes.rowHeight, tasks.size)
    }

    /** Times to draw for [task]: the live preview while dragged, the released times until applied. */
    fun displayRange(task: TimelineTask): TimeRange {
        if (drag?.taskId == task.id) previewRange()?.let { return it }
        released?.let { (id, range) -> if (id == task.id) return range }
        return TimeRange(task.start, task.end)
    }

    /** Vertical offset of the dragged row while reordering, in px. */
    fun draggedRowOffset(): Float {
        val session = drag?.takeIf { it.kind == DragKind.REORDER } ?: return 0f
        return contentY(pointer.y) - session.downContentY
    }

    override fun hitTest(position: Offset): HitTarget {
        val axisWidth = state.viewportWidthPx
        val rows = rowTasks().map { task ->
            val barLeft = state.timeToX(task.start.toDouble())
            val barRight = state.timeToX(task.end.toDouble())
            if (task.id == selectedTaskId) {
                RowHitArea(task.id, barLeft, barRight, labelLeft = null, labelRight = null)
            } else {
                // Same geometry as TaskBar draws, so what you see is what you can tap.
                val clippedLeft = maxOf(barLeft, -axisWidth)
                val clippedWidth = maxOf(minOf(barRight, axisWidth * LayoutConst.BAR_CLIP_WIDTHS) - clippedLeft, 1f)
                val side = barLabelSide(clippedLeft, clippedWidth, axisWidth)
                val labelLeft = barLabelAxisLeft(clippedLeft, side, sizes.labelGap)
                val width = labelWidth(barLabelText(labelText(task), side)) + 2 * sizes.labelPaddingX
                RowHitArea(task.id, barLeft, barRight, labelLeft, labelLeft + width)
            }
        }
        val layout = HitLayout(
            axisLeft = state.axisLeftPx,
            headerHeight = sizes.headerHeight,
            rowHeight = sizes.rowHeight,
            scrollY = state.scrollY,
            handleOutside = sizes.handleOutside,
            handleInside = sizes.handleInside,
        )
        return HitTest.hitTest(position.x, position.y, layout, rows, selectedTaskId)
    }

    // Any task row can be long-pressed to reorder; no selection needed.
    override fun canLongPress(target: HitTarget): Boolean = rowTaskId(target) != null

    override fun canMoveTime(target: HitTarget): Boolean =
        target is HitTarget.BarBody && target.taskId == selectedTaskId

    override fun onLongPress(target: HitTarget) {
        liftedTaskId = rowTaskId(target)
        haptic()
    }

    override fun onLongPressEnd() {
        liftedTaskId = null
    }

    override fun onTap(target: HitTarget) {
        // While editing, any tap on another task only ends editing; tapping again then edits that task.
        val selected = selectedTaskId
        val tappedTaskId = rowTaskId(target)
        if (selected != null && tappedTaskId != null && tappedTaskId != selected) {
            callbacks.onEmptyTap()
            return
        }
        when (target) {
            is HitTarget.BarBody -> tapTask(target.taskId)

            is HitTarget.LeftPaneRow -> tapTask(target.taskId)

            is HitTarget.BarLabel -> tapTask(target.taskId)

            // Blank space in the row being edited does nothing, so a near miss never leaves edit mode.
            is HitTarget.RowBlank -> if (target.taskId != selectedTaskId) tapTask(target.taskId)

            HitTarget.Empty -> callbacks.onEmptyTap()

            else -> Unit
        }
    }

    override fun onDragStart(
        target: HitTarget,
        kind: DragKind,
        downPosition: Offset,
    ) {
        val taskId = when (target) {
            is HitTarget.StartHandle -> target.taskId
            is HitTarget.EndHandle -> target.taskId
            else -> rowTaskId(target)
        } ?: return
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index < 0) return
        val task = tasks[index]
        pointer = downPosition
        drag = DragSession(
            taskId = taskId,
            kind = kind,
            original = TimeRange(task.start, task.end),
            originalIndex = index,
            downTime = state.xToTime(downPosition.x - state.axisLeftPx),
            downContentY = contentY(downPosition.y),
        )
    }

    override fun onDrag(position: Offset) {
        pointer = position
    }

    override fun onDragEnd() {
        val session = drag ?: return
        if (session.kind == DragKind.REORDER) {
            val to = reorderTarget() ?: session.originalIndex
            drag = null
            if (to != session.originalIndex) {
                releasedMove = session.taskId to to
                callbacks.onTaskMove(session.taskId, to)
            }
            // Edge auto-scroll may have moved the view while reordering.
            onVerticalScrollSettled()
            return
        }
        val range = previewRange() ?: session.original
        drag = null
        if (range != session.original) {
            released = session.taskId to range
            callbacks.onTaskTimeChange(session.taskId, range.start, range.end)
        }
    }

    override fun onDragCancel() {
        drag = null
    }

    override fun onZoomEnd() = callbacks.onZoomEnd()

    /** Reports the row whose part is at the top edge; nothing when there are no tasks. */
    override fun onVerticalScrollSettled() {
        if (tasks.isEmpty()) return
        callbacks.onVerticalScrollSettled((state.scrollY / sizes.rowHeight).toInt().coerceIn(0, tasks.lastIndex))
    }

    /**
     * While a drag runs, scrolls when the finger is near an edge of the body: horizontally for move/resize,
     * vertically for reorder, faster closer to the edge. Speed is per second, so the frame rate does not matter.
     */
    suspend fun autoScroll(
        zone: Float,
        maxSpeed: Float,
    ) {
        var lastFrame = 0L
        while (drag != null) {
            withFrameNanos { frameTime ->
                val seconds = if (lastFrame == 0L) 0f else (frameTime - lastFrame) / 1_000_000_000f
                lastFrame = frameTime
                val session = drag ?: return@withFrameNanos
                if (session.kind == DragKind.REORDER) {
                    val y = pointer.y - sizes.headerHeight
                    val speed = GestureMath.autoScrollSpeed(y, 0f, state.viewportHeightPx, zone, maxSpeed)
                    if (speed != 0f) state.scrollBy(0f, -speed * seconds)
                } else {
                    val x = pointer.x - state.axisLeftPx
                    val speed = GestureMath.autoScrollSpeed(x, 0f, state.viewportWidthPx, zone, maxSpeed)
                    if (speed != 0f) state.scrollBy(-speed * seconds, 0f)
                }
            }
        }
    }

    // A tap on a left-pane row or label edits the task when its bar is visible, otherwise only scrolls to it.

    /** A visible bar gets selected (unless already edited); an off-screen one only scrolls into view first. */
    private fun tapTask(taskId: Long) {
        val task = tasks.firstOrNull { it.id == taskId } ?: return
        val barVisible = state.timeToX(task.end.toDouble()) > 0f &&
            state.timeToX(task.start.toDouble()) < state.viewportWidthPx
        when {
            !barVisible -> scope.launch { state.animateTimeToX(task.start.toDouble(), sizes.scrollToTaskMargin) }
            taskId != selectedTaskId -> callbacks.onTaskTap(taskId)
        }
    }

    private fun rowTaskId(target: HitTarget): Long? = when (target) {
        is HitTarget.BarBody -> target.taskId
        is HitTarget.BarLabel -> target.taskId
        is HitTarget.RowBlank -> target.taskId
        is HitTarget.LeftPaneRow -> target.taskId
        else -> null
    }

    private fun contentY(screenY: Float): Float = screenY - sizes.headerHeight + state.scrollY
}
