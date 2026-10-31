package com.sam.gogotimeline.core.text

import java.text.BreakIterator

/** User-perceived characters: Chinese, English, and multi-code-point emoji such as 👍🏽 or 🇹🇼 each count as one. */
internal object Graphemes {
    fun count(text: String): Int = boundaries(text).size - 1

    /** The first [max] characters of [text]. */
    fun take(
        text: String,
        max: Int,
    ): String {
        val boundaries = boundaries(text)
        return if (boundaries.size - 1 <= max) text else text.substring(0, boundaries[max])
    }

    private fun boundaries(text: String): List<Int> {
        val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
        return generateSequence(iterator.first()) { iterator.next().takeIf { it != BreakIterator.DONE } }.toList()
    }
}
