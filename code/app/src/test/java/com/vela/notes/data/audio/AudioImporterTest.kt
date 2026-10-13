package com.vela.notes.data.audio

import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentCleanupResult
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.attachment.StoredAttachment
import com.vela.notes.domain.model.AttachmentType
import com.vela.notes.domain.model.NoteBlockType
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioImporterTest {
    @Test
    fun importsM4aWithDurationAndAudioBlock() = runTest {
        val source = File.createTempFile("audio-import-", ".m4a").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val ids = ArrayDeque(listOf("attachment", "block"))
        val importer = AudioImporter(
            attachmentStore = FakeStore(),
            idGenerator = IdGenerator { ids.removeFirst() },
            clock = VelaClock { 500L },
        )

        val result = importer.import(
            "note",
            source,
            durationMillis = 2_500,
            waveform = byteArrayOf(10, 20),
        )

        assertEquals(AttachmentType.AUDIO, result.attachment.kind)
        assertEquals("audio/mp4", result.attachment.mimeType)
        assertEquals(2_500L, result.attachment.durationMs)
        assertArrayEquals(byteArrayOf(10, 20), result.attachment.waveform)
        assertEquals(NoteBlockType.AUDIO, result.block.type)
        assertEquals("attachment", result.block.attachmentId)
        source.delete()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeDuration() = runTest {
        AudioImporter(FakeStore(), IdGenerator { "id" }, VelaClock { 0 })
            .import("note", File("missing.m4a"), durationMillis = -1)
    }

    private class FakeStore : AttachmentStore {
        override suspend fun import(
            noteId: String,
            attachmentId: String,
            displayName: String,
            source: AttachmentSource,
        ): StoredAttachment {
            val bytes = source.open().use { it.readBytes() }
            return StoredAttachment("$noteId/$attachmentId.m4a", bytes.size.toLong(), "checksum")
        }

        override suspend fun delete(relativePath: String) = Unit
        override suspend fun cleanup(referencedPaths: Set<String>, stagingOlderThanMillis: Long) =
            AttachmentCleanupResult(0, 0)
        override fun resolve(relativePath: String) = File(relativePath)
    }
}
