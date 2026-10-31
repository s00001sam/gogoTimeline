package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeRange
import com.sam.gogotimeline.core.time.at
import com.sam.gogotimeline.data.model.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditSessionTest {
    private val kickoff = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T09:00"), TAIPEI.at("2026-10-31T10:30")))
    private val later = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T14:30")))

    private fun task(
        title: String = "Kickoff meeting",
        schedule: TaskSchedule = kickoff,
    ) = Task(
        id = 1,
        boardId = 1,
        title = title,
        schedule = schedule,
        colorIndex = 3,
        sortOrder = 0,
        createdAt = TAIPEI.at("2026-10-16T09:00"),
        updatedAt = TAIPEI.at("2026-10-20T09:00"),
    )

    private fun EditSession.roundTrip(): EditSession? = SavedStateHandle().also {
        save(it)
    }.let { EditSession.restore(it) }

    @Test
    fun freshSessionIsNotDirty() {
        assertFalse(EditSession.from(task()).isDirty)
    }

    @Test
    fun newTimeMakesItDirty() {
        assertTrue(EditSession.from(task()).withSchedule(later).isDirty)
    }

    @Test
    fun timedSessionSurvivesSaving() {
        val session = EditSession.from(task()).withSchedule(later)
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun movingBackIsNotDirty() {
        assertFalse(EditSession.from(task()).withSchedule(later).withSchedule(kickoff).isDirty)
    }

    @Test
    fun allDaySessionKeepsItsDates() {
        val release = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)))
        val session = EditSession.from(task(schedule = release))
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun allDayBefore1970AndUnicodeTitlesSurvive() {
        val newYearsEve = TaskSchedule.AllDay(DateRange(LocalDate.of(1969, 12, 31), LocalDate.of(1970, 1, 1)))
        val session = EditSession.from(task(title = "跨年 🎆", schedule = newYearsEve))
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun nothingSavedRestoresNull() {
        assertNull(EditSession.restore(SavedStateHandle()))
        val handle = SavedStateHandle()
        EditSession.from(task()).save(handle)
        EditSession.clear(handle)
        assertNull(EditSession.restore(handle))
    }
}
