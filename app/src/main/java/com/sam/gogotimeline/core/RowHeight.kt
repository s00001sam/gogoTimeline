package com.sam.gogotimeline.core

import kotlin.math.roundToInt

/** Row height is stored in dp but shown and adjusted as a multiplier of the default 64dp (spec 8). */
object RowHeight {
    fun toMultiplier(dp: Float): Float = dp / LayoutConst.ROW_HEIGHT_DEFAULT_DP

    fun toDp(multiplier: Float): Float = multiplier * LayoutConst.ROW_HEIGHT_DEFAULT_DP

    /** The settings slider's value on release: the nearest 0.1x within 0.8x–2x. */
    fun roundMultiplier(multiplier: Float): Float {
        // Dividing by the steps per 1x (10) gives exactly 1.3f; multiplying by 0.1f would give 1.3000001f.
        val stepsPerX = (1f / LayoutConst.ROW_HEIGHT_MULTIPLIER_STEP).roundToInt()
        return ((multiplier * stepsPerX).roundToInt().toFloat() / stepsPerX)
            .coerceIn(LayoutConst.ROW_HEIGHT_MULTIPLIER_MIN, LayoutConst.ROW_HEIGHT_MULTIPLIER_MAX)
    }
}
