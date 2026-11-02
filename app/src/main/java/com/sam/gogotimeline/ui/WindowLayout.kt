package com.sam.gogotimeline.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import com.sam.gogotimeline.core.LayoutConst

/**
 * Tablet layout when the current window is at least 600dp wide and 480dp tall. Uses the window, not the
 * device, so split screen and folding re-evaluate; a landscape phone stays on the phone layout.
 */
@Composable
fun isTabletLayout(): Boolean {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return sizeClass.isWidthAtLeastBreakpoint(LayoutConst.TABLET_MIN_WIDTH_DP.toInt()) &&
        sizeClass.isHeightAtLeastBreakpoint(LayoutConst.TABLET_MIN_HEIGHT_DP.toInt())
}

/** Where the task editor goes (spec 7.1): the bottom sheet needs height, the floating card needs width and height. */
enum class EditorLayout {
    BOTTOM_SHEET,
    FLOATING_CARD,
    FULL_PAGE,
    ;

    companion object {
        /** [wide] and [tall] use the tablet thresholds, 600dp wide and 480dp tall. */
        fun of(
            wide: Boolean,
            tall: Boolean,
        ): EditorLayout = when {
            wide && tall -> FLOATING_CARD
            tall -> BOTTOM_SHEET
            else -> FULL_PAGE
        }
    }
}

@Composable
fun editorLayout(): EditorLayout {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return EditorLayout.of(
        wide = sizeClass.isWidthAtLeastBreakpoint(LayoutConst.TABLET_MIN_WIDTH_DP.toInt()),
        tall = sizeClass.isHeightAtLeastBreakpoint(LayoutConst.TABLET_MIN_HEIGHT_DP.toInt()),
    )
}
