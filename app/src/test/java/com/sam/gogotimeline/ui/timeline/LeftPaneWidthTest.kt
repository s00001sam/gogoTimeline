package com.sam.gogotimeline.ui.timeline

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeftPaneWidthTest {
    @Test
    fun phoneCollapsed() {
        assertEquals(100.dp, leftPaneWidth(isTablet = false, expanded = false))
    }

    @Test
    fun phoneExpanded() {
        assertEquals(160.dp, leftPaneWidth(isTablet = false, expanded = true))
    }

    @Test
    fun tabletCollapsed() {
        assertEquals(180.dp, leftPaneWidth(isTablet = true, expanded = false))
    }

    @Test
    fun tabletExpanded() {
        assertEquals(280.dp, leftPaneWidth(isTablet = true, expanded = true))
    }

    @Test
    fun expandingAlwaysWidens() {
        listOf(false, true).forEach { isTablet ->
            assertTrue(leftPaneWidth(isTablet, expanded = true) > leftPaneWidth(isTablet, expanded = false))
        }
    }

    @Test
    fun timelineKeepsRoomOnTheSmallestScreens() {
        val smallPhone = 360.dp
        assertEquals(260.dp, smallPhone - leftPaneWidth(isTablet = false, expanded = false))
        assertEquals(200.dp, smallPhone - leftPaneWidth(isTablet = false, expanded = true))
        val narrowestTablet = 600.dp
        assertEquals(320.dp, narrowestTablet - leftPaneWidth(isTablet = true, expanded = true))
    }
}
