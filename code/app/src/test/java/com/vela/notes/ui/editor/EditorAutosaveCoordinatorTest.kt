package com.vela.notes.ui.editor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorAutosaveCoordinatorTest {
    @Test
    fun savesOnlyLatestValueAfterFiveHundredMilliseconds() = runTest {
        val saved = mutableListOf<String>()
        val coordinator = EditorAutosaveCoordinator<String>(this) { saved += it }

        coordinator.submit("v1")
        runCurrent()
        advanceTimeBy(300)
        coordinator.submit("v2")
        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        assertEquals(emptyList<String>(), saved)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf("v2"), saved)
        coordinator.close()
    }

    @Test
    fun flushSavesImmediatelyAndCancelsPendingTimer() = runTest {
        val saved = mutableListOf<String>()
        val coordinator = EditorAutosaveCoordinator<String>(this) { saved += it }
        coordinator.submit("back-navigation")
        runCurrent()

        coordinator.flush()
        advanceUntilIdle()

        assertEquals(listOf("back-navigation"), saved)
        coordinator.close()
    }

    @Test
    fun slowWritesStaySerializedAndNewerValueFinishesLast() = runTest {
        val firstCanFinish = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val finished = mutableListOf<String>()
        val coordinator = EditorAutosaveCoordinator<String>(this, debounceMillis = 10) { value ->
            started += value
            if (value == "v1") firstCanFinish.await()
            finished += value
        }

        coordinator.submit("v1")
        advanceTimeBy(10)
        runCurrent()
        coordinator.submit("v2")
        val flush = async { coordinator.flush() }
        runCurrent()

        assertEquals(listOf("v1"), started)
        assertFalse(flush.isCompleted)
        firstCanFinish.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("v1", "v2"), finished)
        coordinator.close()
    }
}
