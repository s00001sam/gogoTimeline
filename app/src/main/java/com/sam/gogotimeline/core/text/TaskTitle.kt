package com.sam.gogotimeline.core.text

import com.sam.gogotimeline.core.LayoutConst

/** Name rules shared by the sheet and the repository (spec 7.2); outer spaces do not count. */
object TaskTitle {
    fun length(text: String): Int = Graphemes.count(text)

    fun isValid(title: String): Boolean {
        val trimmed = title.trim()
        return trimmed.isNotEmpty() && length(trimmed) <= LayoutConst.NAME_MAX_LENGTH
    }

    /** While typing: keeps [input] while it fits; a longer paste is cut to the first characters that fit. */
    fun limit(input: String): String {
        if (length(input.trim()) <= LayoutConst.NAME_MAX_LENGTH) return input
        return Graphemes.take(input.trimStart(), LayoutConst.NAME_MAX_LENGTH)
    }

    /** What typing or pasting produces: the name is one line, so line breaks become spaces, then [limit]. */
    fun clean(input: String): String = limit(input.replace(LINE_BREAKS, " "))

    private val LINE_BREAKS = Regex("\\R")
}
