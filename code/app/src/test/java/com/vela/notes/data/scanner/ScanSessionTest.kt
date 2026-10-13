package com.vela.notes.data.scanner

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanSessionTest {
    @Test
    fun reorderKeepsStableIdsAndDeleteRemovesOnlyTheChosenFile() {
        val first = page("a")
        val second = page("b")
        val third = page("c")
        val moved = moveScanPage(listOf(first, second, third), fromIndex = 0, toIndex = 2)

        assertEquals(listOf("b", "c", "a"), moved.map { it.id })
        val deleted = deleteScanPage(moved, "c") as ScanDraftResult.Ready

        assertEquals(listOf("b", "a"), deleted.pages.map { it.id })
        assertFalse(third.imageFile.exists())
        assertFalse(third.thumbnailFile.exists())
        assertTrue(first.imageFile.exists())
        assertTrue(second.imageFile.exists())
        deleted.pages.forEach { discardScanPages(listOf(it)) }
    }

    @Test
    fun lastPageRequiresConfirmationAndThenDeletesTheFile() {
        val only = page("only")
        val blocked = deleteScanPage(listOf(only), "only")

        assertEquals(ScanDraftResult.ConfirmDiscardLast, blocked)
        assertTrue(only.imageFile.exists())

        val discarded = deleteScanPage(listOf(only), "only", discardLast = true) as ScanDraftResult.Ready
        assertTrue(discarded.pages.isEmpty())
        assertFalse(only.imageFile.exists())
    }

    @Test
    fun fiftyFirstPageIsRejectedAndDiscardDeletesEveryTempFile() {
        val pages = List(MAX_SCAN_PAGES) { index -> page("page-$index") }
        val extra = page("extra")
        val rejected = addScanPage(pages, extra)

        assertEquals(ScanDraftResult.LimitReached, rejected)
        assertTrue(extra.imageFile.exists())

        val rotated = updateScanPage(pages, "page-1") { page ->
            page.copy(quarterTurns = 1, filter = ScanFilter.GRAYSCALE)
        }
        assertEquals(1, rotated[1].quarterTurns)
        assertEquals(ScanFilter.GRAYSCALE, rotated[1].filter)
        discardScanPages(rotated + extra)
        assertTrue((rotated + extra).all { !it.imageFile.exists() && !it.thumbnailFile.exists() })
    }

    private fun page(id: String): ScanPage {
        val image = File.createTempFile("scan-$id-", ".jpg")
        val thumb = File.createTempFile("thumb-$id-", ".jpg")
        image.writeBytes(byteArrayOf(1))
        thumb.writeBytes(byteArrayOf(2))
        return ScanPage(id = id, imageFile = image, thumbnailFile = thumb)
    }
}
