package com.vela.notes.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCornerGeometryTest {
    @Test
    fun clampsCornerInsideTheImage() {
        val clamped = clampToImage(-20f, 500f, 200, 100)

        assertEquals(ScanPoint(0f, 100f), clamped)
    }

    @Test
    fun movingOneCornerLeavesTheOthersUntouched() {
        val original = defaultDocumentQuad(200, 100)
        val moved = original.withCorner(1, ScanPoint(150f, 12f))

        assertEquals(ScanPoint(150f, 12f), moved.topRight)
        assertEquals(original.topLeft, moved.topLeft)
        assertEquals(original.bottomRight, moved.bottomRight)
        assertEquals(original.bottomLeft, moved.bottomLeft)
    }

    @Test
    fun mapsTouchIntoImageSpaceAndBack() {
        val frame = fittedImageFrame(200f, 100f, 100, 100)

        assertEquals(50f, frame.left)
        assertEquals(0f, frame.top)
        assertEquals(ScanPoint(0f, 0f), frame.toImagePoint(50f, 0f, 100, 100))
        assertEquals(ScanPoint(100f, 100f), frame.toImagePoint(400f, 200f, 100, 100))
        assertEquals(ScanPoint(50f, 0f), frame.toScreenPoint(ScanPoint(0f, 0f), 100, 100))
    }

    @Test
    fun magnifierSampleStaysInsideTheImage() {
        val sample = magnifierSampleSquare(ScanPoint(2f, 90f), 100, 100, 20)

        assertTrue(sample.left >= 0f)
        assertTrue(sample.top >= 0f)
        assertTrue(sample.left + sample.width <= 100f)
        assertTrue(sample.top + sample.height <= 100f)
    }
}
