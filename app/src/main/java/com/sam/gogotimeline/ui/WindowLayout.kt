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
