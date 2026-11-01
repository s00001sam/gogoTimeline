package com.sam.gogotimeline.ui.board

import androidx.lifecycle.SavedStateHandle
import com.sam.gogotimeline.core.time.DateRange
import com.sam.gogotimeline.core.time.SheetTimeEdit
import com.sam.gogotimeline.core.time.TAIPEI
import com.sam.gogotimeline.core.time.TaskSchedule
import com.sam.gogotimeline.core.time.TimeError
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
        notes = "Agenda: goals, roles, timeline",
    )

    // The task sits on row 1 of the board when editing starts.
    private fun fromTask(task: Task): EditSession = EditSession.from(task, row = 1)

    private fun EditSession.roundTrip(): EditSession? = SavedStateHandle().also {
        save(it)
    }.let { EditSession.restore(it) }

    @Test
    fun newTaskStartsEmptyAndUnchanged() {
        val session = EditSession.newTask(kickoff, rowCount = 3, colorIndex = 4)
        assertTrue(session.draft.isNew)
        assertEquals("", session.draft.title)
        assertFalse(session.isDirty)
        assertFalse(session.isValid)
    }

    @Test
    fun newTaskWithANameIsDirtyAndValid() {
        val session = EditSession.newTask(kickoff, rowCount = 3, colorIndex = 4).withTitle("Design review")
        assertTrue(session.isDirty)
        assertTrue(session.isValid)
    }

    @Test
    fun newTaskDraftSurvivesProcessDeath() {
        val session = EditSession.newTask(kickoff, rowCount = 3, colorIndex = 6).withTitle("Design review")
        val restored = session.roundTrip()!!
        assertTrue(restored.draft.isNew)
        assertEquals(session, restored)
    }

    @Test
    fun newRowIsAChangeThatSurvivesProcessDeath() {
        val moved = fromTask(task()).withRow(0)
        assertTrue(moved.isDirty)
        assertEquals(1, moved.originalRow)
        assertEquals(moved, moved.roundTrip())
    }

    @Test
    fun movingBackToTheSameRowIsNotAChange() {
        assertFalse(fromTask(task()).withRow(4).withRow(1).isDirty)
    }

    @Test
    fun newTaskStartsAsTheLastRowAndCanMove() {
        val session = EditSession.newTask(kickoff, rowCount = 3, colorIndex = 0)
        assertEquals(3, session.draftRow)
        // Only moving it, without a name, already counts as a change worth asking about.
        assertTrue(session.withRow(0).isDirty)
    }

    @Test
    fun freshSessionIsNotDirty() {
        assertFalse(fromTask(task()).isDirty)
    }

    @Test
    fun newTimeMakesItDirty() {
        assertTrue(fromTask(task()).withSchedule(later).isDirty)
    }

    @Test
    fun timedSessionSurvivesSaving() {
        val session = fromTask(task()).withSchedule(later)
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun movingBackIsNotDirty() {
        assertFalse(fromTask(task()).withSchedule(later).withSchedule(kickoff).isDirty)
    }

    @Test
    fun allDaySessionKeepsItsDates() {
        val release = TaskSchedule.AllDay(DateRange(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)))
        val session = fromTask(task(schedule = release))
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun uncheckingAllDayRightAwayRestoresTheTimes() {
        val session = fromTask(task()).withAllDay(true, TAIPEI).withAllDay(false, TAIPEI)
        assertEquals(kickoff, session.draft.schedule)
    }

    @Test
    fun uncheckingAfterADateChangeLeavesMidnights() {
        val checked = fromTask(task()).withAllDay(true, TAIPEI)
        val moved = checked.withSheetSchedule(
            SheetTimeEdit.withEndDate(checked.draft.schedule, LocalDate.of(2026, 11, 2), TAIPEI),
        )
        val unchecked = moved.withAllDay(false, TAIPEI)
        assertEquals(
            TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T00:00"), TAIPEI.at("2026-11-03T00:00"))),
            unchecked.draft.schedule,
        )
    }

    @Test
    fun invalidTimesKeepTheLastValidOnesForTheBar() {
        val invalid = TaskSchedule.Timed(TimeRange(TAIPEI.at("2026-10-31T13:00"), TAIPEI.at("2026-10-31T10:30")))
        val session = fromTask(task()).withSheetSchedule(later).withSheetSchedule(invalid)
        assertEquals(later, session.lastValidSchedule)
        assertEquals(TimeError.END_NOT_AFTER_START, session.timeError)
        val restored = session.roundTrip()!!
        assertEquals(invalid, restored.draft.schedule)
        assertEquals(kickoff, restored.lastValidSchedule)
    }

    @Test
    fun allDayBefore1970AndUnicodeTitlesSurvive() {
        val newYearsEve = TaskSchedule.AllDay(DateRange(LocalDate.of(1969, 12, 31), LocalDate.of(1970, 1, 1)))
        val session = fromTask(task(title = "跨年 🎆", schedule = newYearsEve)).withNotes("倒數\n  - 煙火 🎆\n")
        assertEquals(session, session.roundTrip())
    }

    @Test
    fun newNotesMakeItDirty() {
        assertTrue(fromTask(task()).withNotes("Bring laptops").isDirty)
    }

    @Test
    fun nothingSavedRestoresNull() {
        assertNull(EditSession.restore(SavedStateHandle()))
        val handle = SavedStateHandle()
        fromTask(task()).save(handle)
        EditSession.clear(handle)
        assertNull(EditSession.restore(handle))
    }
}
