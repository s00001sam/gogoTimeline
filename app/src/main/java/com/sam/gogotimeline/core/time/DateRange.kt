package com.sam.gogotimeline.core.time

import java.time.LocalDate

/** Dates of an all-day task, end exclusive; the same dates in every time zone. */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
)
