package com.sam.gogotimeline.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorLayoutTest {
    @Test
    fun portraitPhoneKeepsTheBottomSheetAndTabletsUseTheCard() {
        assertEquals(EditorLayout.BOTTOM_SHEET, EditorLayout.of(wide = false, tall = true))
        assertEquals(EditorLayout.FLOATING_CARD, EditorLayout.of(wide = true, tall = true))
    }

    @Test
    fun landscapePhoneIsTooShortForTheSheetButWideEnoughForTheCard() {
        assertEquals(EditorLayout.FLOATING_CARD, EditorLayout.of(wide = true, tall = false))
    }

    @Test
    fun shortAndNarrowFallsBackToAFullPage() {
        assertEquals(EditorLayout.FULL_PAGE, EditorLayout.of(wide = false, tall = false))
    }
}
