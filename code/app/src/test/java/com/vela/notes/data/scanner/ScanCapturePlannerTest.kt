package com.vela.notes.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCapturePlannerTest {
    private val planner = ScanCapturePlanner(stableFramesRequired = 3, countdownStart = 3)

    @Test
    fun missingQuadStaysSearching() {
        val decision = planner.onFrame(null, 480, 640)

        assertEquals(ScanPhase.SEARCHING, decision.guide.phase)
        assertFalse(decision.shouldCapture)
    }

    @Test
    fun stableQuadCountsDownThenCapturesOnce() {
        val decisions = List(8) { planner.onFrame(page(), 480, 640) }

        assertEquals(ScanPhase.TRACKING, decisions[0].guide.phase)
        assertEquals(ScanPhase.TRACKING, decisions[1].guide.phase)
        assertEquals(3, decisions[2].guide.countdown)
        assertEquals(ScanPhase.STABLE, decisions[2].guide.phase)
        assertEquals(2, decisions[3].guide.countdown)
        assertEquals(1, decisions[4].guide.countdown)
        assertTrue(decisions[5].shouldCapture)
        assertEquals(ScanPhase.CAPTURED, decisions[5].guide.phase)
        assertFalse(decisions[6].shouldCapture)
        assertFalse(decisions[7].shouldCapture)
    }

    @Test
    fun movementResetsStability() {
        planner.onFrame(page(), 480, 640)
        planner.onFrame(page(), 480, 640)

        val reset = planner.onFrame(page().translate(0f, 40f), 480, 640)

        assertEquals(ScanPhase.TRACKING, reset.guide.phase)
        assertNull(reset.guide.countdown)
        assertFalse(reset.shouldCapture)
    }

    @Test
    fun losingQuadRearmsAutoCapture() {
        repeat(6) { planner.onFrame(page(), 480, 640) }
        planner.onFrame(null, 480, 640)

        val again = List(6) { planner.onFrame(page(), 480, 640) }

        assertEquals(ScanPhase.SEARCHING, planner.onFrame(null, 480, 640).guide.phase)
        assertTrue(again[5].shouldCapture)
    }

    @Test
    fun manualCaptureWorksWithoutADetectedEdge() {
        val decision = planner.captureManually(null, 480, 640)

        assertTrue(decision.shouldCapture)
        assertEquals(ScanPhase.CAPTURED, decision.guide.phase)
        assertNull(decision.guide.quad)
        assertFalse(planner.onFrame(page(), 480, 640).shouldCapture)
    }
}
