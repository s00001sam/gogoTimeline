package com.sam.gogotimeline.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTitleTest {
    @Test
    fun ordinaryTitleIsValid() {
        assertTrue(TaskTitle.isValid("Kickoff meeting"))
    }

    @Test
    fun outerSpacesDoNotCount() {
        assertEquals(2, TaskTitle.length("  Hi  ".trim()))
        assertTrue(TaskTitle.isValid("  Hi  "))
    }

    @Test
    fun fortyCharactersIsTheLimit() {
        val forty = "a".repeat(40)
        assertTrue(TaskTitle.isValid(forty))
        assertEquals(forty, TaskTitle.limit(forty + "b"))
    }

    @Test
    fun spacesAroundAFullTitleStillFit() {
        val padded = "  " + "a".repeat(40) + "  "
        assertEquals(padded, TaskTitle.limit(padded))
    }

    @Test
    fun emojiAndChineseCountAsOneEach() {
        assertEquals(4, TaskTitle.length("👍🏽🇹🇼中a"))
        val fortyFlags = "🇹🇼".repeat(40)
        assertTrue(TaskTitle.isValid(fortyFlags))
        assertEquals(fortyFlags, TaskTitle.limit(fortyFlags + "🇹🇼"))
    }

    @Test
    fun blankTitleIsInvalid() {
        assertFalse(TaskTitle.isValid(""))
        assertFalse(TaskTitle.isValid("   "))
    }

    @Test
    fun longPasteIsCutToForty() {
        assertEquals("a".repeat(40), TaskTitle.limit("  " + "a".repeat(50)))
    }

    @Test
    fun cleanKeepsASingleLineName() {
        assertEquals("Kickoff meeting", TaskTitle.clean("Kickoff meeting"))
    }

    @Test
    fun trailingLineBreakBecomesASpaceThatDoesNotCount() {
        val cleaned = TaskTitle.clean("Kickoff\n")
        assertEquals("Kickoff ", cleaned)
        assertEquals(7, TaskTitle.length(cleaned.trim()))
    }

    @Test
    fun pastedLinesJoinWithSpacesAndStayWithinForty() {
        assertEquals("Plan the release", TaskTitle.clean("Plan\r\nthe\nrelease"))
        assertEquals(40, TaskTitle.length(TaskTitle.clean("a".repeat(30) + "\n" + "b".repeat(30))))
    }
}
