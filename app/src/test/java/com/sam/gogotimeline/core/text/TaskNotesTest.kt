package com.sam.gogotimeline.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskNotesTest {
    @Test
    fun multiLineNotesAreValid() {
        assertTrue(TaskNotes.isValid("Agenda:\n- goals\n- roles"))
    }

    @Test
    fun savingDropsTrailingBlanksButKeepsIndentation() {
        assertEquals("  - goals\n  - roles", TaskNotes.normalize("  - goals\n  - roles \n\n"))
    }

    @Test
    fun fiveHundredCharactersIsTheLimit() {
        val full = "a".repeat(500)
        assertTrue(TaskNotes.isValid(full))
        assertFalse(TaskNotes.isValid(full + "b"))
        assertEquals(full, TaskNotes.limit(full + "b"))
    }

    @Test
    fun emptyNotesAreValid() {
        assertTrue(TaskNotes.isValid(""))
        assertEquals("", TaskNotes.normalize("  \n "))
    }

    @Test
    fun longEmojiPasteIsCutToFiveHundred() {
        val pasted = "🇹🇼".repeat(600)
        assertEquals(500, TaskNotes.length(TaskNotes.limit(pasted)))
    }

    @Test
    fun trailingBlanksDoNotCountTowardTheLimit() {
        assertTrue(TaskNotes.isValid("a".repeat(500) + "\n\n  "))
    }
}
