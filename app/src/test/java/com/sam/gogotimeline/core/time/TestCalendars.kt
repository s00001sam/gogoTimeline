package com.sam.gogotimeline.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/** No DST, UTC+8: catches snapping to UTC instead of local midnight. */
val TAIPEI = CalendarContext(ZoneId.of("Asia/Taipei"), DayOfWeek.SUNDAY)

/** DST in 2026: 03-08 skips 02:00 (23-hour day), 11-01 repeats 01:00 (25-hour day). */
val NEW_YORK = CalendarContext(ZoneId.of("America/New_York"), DayOfWeek.SUNDAY)

/** UTC+5:30 with a Monday week start. */
val KOLKATA = CalendarContext(ZoneId.of("Asia/Kolkata"), DayOfWeek.MONDAY)

/** DST skips midnight: 2026-09-06 starts at 01:00 and lasts 23 hours. */
val SANTIAGO = CalendarContext(ZoneId.of("America/Santiago"), DayOfWeek.MONDAY)

/** Local wall time to epoch ms; a repeated DST hour resolves to its first occurrence. */
fun CalendarContext.at(local: String): Long = LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

/** Exact instant with an explicit offset, e.g. "2026-11-01T01:30-05:00". */
fun atOffset(text: String): Long = OffsetDateTime.parse(text).toInstant().toEpochMilli()

/** Epoch ms to local wall time text, e.g. "2026-10-30T14:15", for readable assertions. */
fun CalendarContext.local(time: Long): String = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime().toString()
