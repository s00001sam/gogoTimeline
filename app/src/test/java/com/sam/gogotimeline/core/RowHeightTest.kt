package com.sam.gogotimeline.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RowHeightTest {
    @Test
    fun multiplierRoundsToATenthAndMapsToDp() {
        // Exactly 1.3f, not 1.3000001f, so the saved dp has no float noise.
        assertEquals(1.3f, RowHeight.roundMultiplier(1.27f))
        assertEquals(83.2f, RowHeight.toDp(1.3f), EPSILON)
        assertEquals(1.5f, RowHeight.toMultiplier(96f), EPSILON)
    }

    @Test
    fun endsAndTheDefaultStayPut() {
        assertEquals(0.8f, RowHeight.roundMultiplier(0.8f), EPSILON)
        assertEquals(2f, RowHeight.roundMultiplier(2f), EPSILON)
        assertEquals(1f, RowHeight.toMultiplier(LayoutConst.ROW_HEIGHT_DEFAULT_DP), EPSILON)
    }

    @Test
    fun valuesOutsideTheRangeAreClamped() {
        assertEquals(0.8f, RowHeight.roundMultiplier(0f), EPSILON)
        assertEquals(2f, RowHeight.roundMultiplier(7f), EPSILON)
        // A row saved at 48dp under the old range shows as 0.75x and is clamped up once moved.
        assertEquals(0.8f, RowHeight.roundMultiplier(RowHeight.toMultiplier(48f)), EPSILON)
    }

    private companion object {
        const val EPSILON = 1e-4f
    }
}
