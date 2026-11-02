package com.sam.gogotimeline.data.model

import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeLimits
import com.sam.gogotimeline.core.time.TimeRange
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskValidatorTest {
    @Test
    fun rejectsInvalidScheduleColorAndTimeLimits() {
        val valid = TaskSchedule.Timed(TimeRange(TimeLimits.MIN, TimeLimits.MIN + 60_000L * 15))
        val invalidDuration = TaskSchedule.Timed(TimeRange(TimeLimits.MIN, TimeLimits.MIN + 1))
        val outside = TaskSchedule.Timed(TimeRange(TimeLimits.MIN - 1, TimeLimits.MIN + 60_000L * 15))
        val invalidDates = TaskSchedule.AllDay(
            DateRange(LocalDate.of(2100, 12, 31), LocalDate.of(2101, 1, 2)),
        )

        TaskValidator.requireValid(valid, colorIndex = 0)
        assertTrue(runCatching { TaskValidator.requireValid(invalidDuration, 0) }.isFailure)
        assertTrue(runCatching { TaskValidator.requireValid(outside, 0) }.isFailure)
        assertTrue(runCatching { TaskValidator.requireValid(valid, -1) }.isFailure)
        assertTrue(runCatching { TaskValidator.requireValid(invalidDates, 0) }.isFailure)
    }
}
