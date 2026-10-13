package com.vela.notes.data.scanner

import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanOcrRunnerTest {
    @Test
    fun storesVietnameseTextOutsideTheNoteBodyAndReportsProgress() = runTest {
        val progress = mutableListOf<Pair<Int, Int>>()
        val text = ScanOcrRunner<String> { page ->
            if (page == "blank") "   " else "Xin chào Hà Nội"
        }.recognize(listOf("page", "blank")) { completed, total -> progress += completed to total }

        assertEquals("Xin chào Hà Nội", text)
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progress)
        val updated = sampleDocument().withAttachmentOcr("scan", text)
        assertEquals("Xin chào Hà Nội", updated.attachments.single().ocrText)
        assertEquals(sampleDocument().blocks, updated.blocks)
    }

    @Test
    fun emptyPagesProduceNoOcrText() = runTest {
        val text = ScanOcrRunner<String> { "" }.recognize(listOf("empty")) { _, _ -> }
        assertNull(text)
    }

    @Test
    fun failurePropagatesAndCancellationStopsLaterPages() = runTest {
        val failure = runCatching {
            ScanOcrRunner<String> { error("ocr failed") }.recognize(listOf("page")) { _, _ -> }
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)

        val started = CompletableDeferred<Unit>()
        val seen = mutableListOf<String>()
        val job = launch {
            ScanOcrRunner<String> { page ->
                seen += page
                if (page == "second") {
                    started.complete(Unit)
                    awaitCancellation()
                }
                "trang"
            }.recognize(listOf("first", "second", "third")) { _, _ -> }
        }
        started.await()
        job.cancel()
        job.join()
        assertEquals(listOf("first", "second"), seen)
    }

    private fun sampleDocument() = NoteDocument(
        note = Note(id = "note", title = "Việc", createdAt = 1, updatedAt = 1),
        blocks = listOf(
            NoteBlock(
                id = "text",
                noteId = "note",
                type = NoteBlockType.TEXT,
                position = 0,
                textContent = "Nội dung gốc",
                createdAt = 1,
                updatedAt = 1,
            ),
        ),
        attachments = listOf(
            Attachment(
                id = "scan",
                noteId = "note",
                kind = AttachmentType.SCAN_PDF,
                relativePath = "note/scan.pdf",
                displayName = "Tài liệu đã quét.pdf",
                mimeType = "application/pdf",
                sizeBytes = 4,
                pageCount = 1,
                checksum = "checksum",
                createdAt = 1,
            ),
        ),
    )
}
