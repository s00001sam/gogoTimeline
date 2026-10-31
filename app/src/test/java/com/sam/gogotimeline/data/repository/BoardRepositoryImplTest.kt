package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.data.datasource.BoardLocalDataSource
import com.sam.gogotimeline.data.local.BoardEntity
import com.sam.gogotimeline.data.model.Board
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardRepositoryImplTest {
    private val boards = mockk<BoardLocalDataSource>(relaxed = true)
    private val repository = BoardRepositoryImpl(boards, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC))

    @Test
    fun openingABoardRecordsNow() = runTest {
        repository.markOpened(1)
        coVerify { boards.updateLastOpenedAt(1, NOW) }
    }

    @Test
    fun observedEntityBecomesABoard() = runTest {
        coEvery { boards.observeBoard(1) } returns flowOf(BoardEntity(1, "Sample Project", 3, NOW))
        assertEquals(Board(1, "Sample Project", 3, NOW), repository.observeBoard(1).first())
    }

    @Test
    fun anchorCanBeCleared() = runTest {
        repository.saveAnchor(1, null)
        coVerify { boards.updateAnchor(1, null) }
    }

    @Test
    fun deletedBoardOrNoBoardIsNull() = runTest {
        coEvery { boards.observeBoard(1) } returns flowOf(null)
        coEvery { boards.getMostRecentBoardId() } returns null
        assertNull(repository.observeBoard(1).first())
        assertNull(repository.getStartBoardId())
    }

    private companion object {
        const val NOW = 1_793_412_000_000L
    }
}
