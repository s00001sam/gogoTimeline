package com.sam.gogotimeline.ui.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

class BarLabelSideTest {
    private fun side(
        barLeft: Float,
        barWidth: Float,
        axisWidth: Float = AXIS,
    ) = barLabelSide(barLeft, barWidth, axisWidth)

    @Test
    fun visibleBarKeepsItsLabel() {
        assertEquals(BarLabelSide.VISIBLE, side(100f, 200f))
    }

    @Test
    fun barEntirelyLeftGetsTheEarlierArrow() {
        assertEquals(BarLabelSide.EARLIER, side(-500f, 200f))
    }

    @Test
    fun barEntirelyRightGetsTheLaterArrow() {
        assertEquals(BarLabelSide.LATER, side(1_200f, 200f))
    }

    @Test
    fun halfVisibleBarIsStillVisible() {
        assertEquals(BarLabelSide.VISIBLE, side(-100f, 200f))
    }

    @Test
    fun rightEdgeExactlyAtTheLeftEdgeIsEarlier() {
        assertEquals(BarLabelSide.EARLIER, side(-200f, 200f))
    }

    @Test
    fun leftEdgeExactlyAtTheRightEdgeIsLater() {
        assertEquals(BarLabelSide.LATER, side(AXIS, 200f))
    }

    @Test
    fun leftEdgeJustInsideIsVisible() {
        assertEquals(BarLabelSide.VISIBLE, side(999.9f, 200f))
    }

    @Test
    fun onePixelBarJustOffScreen() {
        assertEquals(BarLabelSide.EARLIER, side(-1f, 1f))
    }

    @Test
    fun clippedLongBarIsVisible() {
        assertEquals(BarLabelSide.VISIBLE, side(-AXIS, 4_000f))
    }

    @Test
    fun beforeLayoutTheAxisHasNoWidth() {
        assertEquals(BarLabelSide.LATER, side(0f, 200f, axisWidth = 0f))
        assertEquals(BarLabelSide.EARLIER, side(-300f, 200f, axisWidth = 0f))
    }

    private companion object {
        const val AXIS = 1_000f
    }
}
