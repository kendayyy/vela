package com.vela.notes.data.scanner

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameAnalysisGateTest {
    @Test
    fun overlappingFramesAreDroppedUntilRelease() {
        val gate = FrameAnalysisGate()
        assertTrue(gate.tryAcquire())

        val dropped = AtomicInteger(0)
        val accepted = AtomicInteger(0)
        val threads = List(32) {
            thread {
                if (gate.tryAcquire()) accepted.incrementAndGet() else dropped.incrementAndGet()
            }
        }
        threads.forEach { it.join() }

        assertEquals(0, accepted.get())
        assertEquals(32, dropped.get())
        assertEquals(1, gate.accepted)
        assertEquals(32, gate.dropped)

        gate.release()
        assertTrue(gate.tryAcquire())
        assertEquals(2, gate.accepted)
    }

    @Test
    fun slowAnalysisKeepsOnlyOneFrameInFlight() {
        val gate = FrameAnalysisGate()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)

        thread {
            check(gate.tryAcquire())
            started.countDown()
            release.await()
            gate.release()
            finished.countDown()
        }
        started.await()

        repeat(20) { assertEquals(false, gate.tryAcquire()) }
        assertEquals(1, gate.accepted)
        assertEquals(20, gate.dropped)

        release.countDown()
        finished.await()
        assertTrue(gate.tryAcquire())
    }
}
