package com.sam.gogotimeline.core.text

import com.sam.gogotimeline.core.LayoutConst
import java.text.BreakIterator

/**
 * Name rules shared by the sheet and the repository (spec 7.2): counted in user-perceived characters, so
 * Chinese, English, and multi-code-point emoji such as 👍🏽 or 🇹🇼 each count as one; outer spaces do not count.
 */
object TaskTitle {
    fun length(text: String): Int = boundaries(text).size - 1

    fun isValid(title: String): Boolean {
        val trimmed = title.trim()
        return trimmed.isNotEmpty() && length(trimmed) <= LayoutConst.NAME_MAX_LENGTH
    }

    /** While typing: keeps [input] while it fits; a longer paste is cut to the first characters that fit. */
    fun limit(input: String): String {
        if (length(input.trim()) <= LayoutConst.NAME_MAX_LENGTH) return input
        val text = input.trimStart()
        return text.substring(0, boundaries(text)[LayoutConst.NAME_MAX_LENGTH])
    }

    /** What typing or pasting produces: the name is one line, so line breaks become spaces, then [limit]. */
    fun clean(input: String): String = limit(input.replace(LINE_BREAKS, " "))

    private val LINE_BREAKS = Regex("\\R")

    private fun boundaries(text: String): List<Int> {
        val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
        return generateSequence(iterator.first()) { iterator.next().takeIf { it != BreakIterator.DONE } }.toList()
    }
}
