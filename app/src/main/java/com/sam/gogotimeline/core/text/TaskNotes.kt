package com.sam.gogotimeline.core.text

import com.sam.gogotimeline.core.LayoutConst

/** Notes rules (spec 7.2): may be empty and span lines; trailing blanks are dropped on save, indentation is kept. */
object TaskNotes {
    fun length(text: String): Int = Graphemes.count(text)

    fun isValid(notes: String): Boolean = length(normalize(notes)) <= LayoutConst.NOTES_MAX_LENGTH

    /** While typing: a longer paste is cut to the first characters that fit. */
    fun limit(input: String): String = Graphemes.take(input, LayoutConst.NOTES_MAX_LENGTH)

    fun normalize(notes: String): String = notes.trimEnd()
}
