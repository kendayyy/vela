package com.vela.notes.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanGeometryTest {
    @Test
    fun ordersCornersFromAnyStartingPoint() {
        val ordered = orderCorners(
            listOf(
                ScanPoint(290f, 400f),
                ScanPoint(20f, 20f),
                ScanPoint(30f, 390f),
                ScanPoint(300f, 30f),
            ),
        )

        assertEquals(ScanPoint(20f, 20f), ordered?.topLeft)
        assertEquals(ScanPoint(300f, 30f), ordered?.topRight)
        assertEquals(ScanPoint(290f, 400f), ordered?.bottomRight)
        assertEquals(ScanPoint(30f, 390f), ordered?.bottomLeft)
    }

    @Test
    fun rejectsIncompleteAndBowtiePolygons() {
        assertNull(orderCorners(listOf(ScanPoint(0f, 0f), ScanPoint(1f, 1f))))
        val bowtie = DocumentQuad(
            ScanPoint(0f, 0f),
            ScanPoint(100f, 100f),
            ScanPoint(0f, 100f),
            ScanPoint(100f, 0f),
        )
        assertFalse(isValidDocumentQuad(bowtie, 200, 200))
    }

    @Test
    fun acceptsPageAndRejectsTinyOrOutOfBoundsQuads() {
        assertTrue(isValidDocumentQuad(page(), 480, 640))
        assertFalse(
            isValidDocumentQuad(
                DocumentQuad(
                    ScanPoint(0f, 0f),
                    ScanPoint(20f, 0f),
                    ScanPoint(20f, 20f),
                    ScanPoint(0f, 20f),
                ),
                480,
                640,
            ),
        )
        assertFalse(isValidDocumentQuad(page().translate(500f, 0f), 480, 640))
    }

    @Test
    fun rotatedRectangleStillOrdersAndValidates() {
        val rotated = orderCorners(
            listOf(
                ScanPoint(340f, 400f),
                ScanPoint(200f, 90f),
                ScanPoint(120f, 330f),
                ScanPoint(400f, 160f),
            ),
        )

        assertEquals(ScanPoint(200f, 90f), rotated?.topLeft)
        assertEquals(ScanPoint(400f, 160f), rotated?.topRight)
        assertEquals(ScanPoint(340f, 400f), rotated?.bottomRight)
        assertEquals(ScanPoint(120f, 330f), rotated?.bottomLeft)
        assertTrue(rotated != null && isValidDocumentQuad(rotated, 480, 480))
    }
}

internal fun page(): DocumentQuad = DocumentQuad(
    ScanPoint(40f, 50f),
    ScanPoint(420f, 70f),
    ScanPoint(400f, 560f),
    ScanPoint(60f, 540f),
)
