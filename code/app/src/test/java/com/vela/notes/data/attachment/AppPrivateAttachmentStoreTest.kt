package com.vela.notes.data.attachment

import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppPrivateAttachmentStoreTest {
    private lateinit var sandbox: File
    private lateinit var attachments: File
    private lateinit var staging: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        sandbox = Files.createTempDirectory(context.cacheDir.toPath(), "attachment-store-test").toFile()
        attachments = File(sandbox, "attachments")
        staging = File(sandbox, "staging")
    }

    @After
    fun tearDown() {
        sandbox.deleteRecursively()
    }

    @Test
    fun importCommitsFileAndReturnsMetadata() = runTest {
        val bytes = "Vela attachment".encodeToByteArray()
        val store = AppPrivateAttachmentStore(attachments, staging)

        val result = store.import("note-1", "attachment-1", "Photo.JPEG") {
            ByteArrayInputStream(bytes)
        }

        assertEquals("note-1/attachment-1.jpeg", result.relativePath)
        assertEquals(bytes.size.toLong(), result.sizeBytes)
        assertEquals("2ec1198118aa7d8bbdf075f97fa8dd65762430a2f28a583ed8d84c291e8820cb", result.checksumSha256)
        assertArrayEquals(bytes, store.resolve(result.relativePath).readBytes())
        assertTrue(staging.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun importRejectsOversizeAndRemovesPartialFile() = runTest {
        val store = AppPrivateAttachmentStore(attachments, staging, maxFileBytes = 4)

        expectThrows(AttachmentStorageException.FileTooLarge::class.java) {
            store.import("note-1", "attachment-1", "large.pdf") {
                ByteArrayInputStream(ByteArray(5))
            }
        }

        assertFalse(File(attachments, "note-1/attachment-1.pdf").exists())
        assertTrue(staging.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun storageFailureRemovesTheStagedFile() = runTest {
        val store = AppPrivateAttachmentStore(attachments, staging)

        expectThrows(AttachmentStorageException.StorageFailure::class.java) {
            store.import("note-1", "attachment-1", "notes.txt") {
                object : InputStream() {
                    override fun read(): Int = throw IOException("No space left on device")
                }
            }
        }

        assertTrue(staging.listFiles().orEmpty().isEmpty())
        assertFalse(attachments.walk().any { it.isFile })
    }

    @Test
    fun cleanupKeepsReferencesAndDeletesOrphansAndExpiredStaging() = runTest {
        val store = AppPrivateAttachmentStore(attachments, staging)
        val kept = store.import("note-1", "keep", "keep.txt") {
            ByteArrayInputStream(byteArrayOf(1))
        }
        val orphan = store.import("note-1", "orphan", "orphan.txt") {
            ByteArrayInputStream(byteArrayOf(2))
        }
        staging.mkdirs()
        val oldPart = File(staging, "old.part").apply {
            writeText("partial")
            setLastModified(100)
        }
        val freshPart = File(staging, "fresh.part").apply {
            writeText("partial")
            setLastModified(300)
        }

        val result = store.cleanup(setOf(kept.relativePath), stagingOlderThanMillis = 200)

        assertEquals(1, result.deletedOrphans)
        assertEquals(1, result.deletedStagingFiles)
        assertTrue(store.resolve(kept.relativePath).exists())
        assertFalse(store.resolve(orphan.relativePath).exists())
        assertFalse(oldPart.exists())
        assertTrue(freshPart.exists())
    }

    @Test
    fun resolveRejectsTraversalAndBackslashes() {
        val store = AppPrivateAttachmentStore(attachments, staging)

        assertThrows(AttachmentStorageException.InvalidRelativePath::class.java) {
            store.resolve("../outside.txt")
        }
        assertThrows(AttachmentStorageException.InvalidRelativePath::class.java) {
            store.resolve("note\\outside.txt")
        }
    }

    @Test
    fun importRejectsUnsafeIdentifiers() = runTest {
        val store = AppPrivateAttachmentStore(attachments, staging)

        expectThrows(AttachmentStorageException.InvalidIdentifier::class.java) {
            store.import("../note", "attachment", "file.txt") {
                ByteArrayInputStream(byteArrayOf(1))
            }
        }
    }

    @Test
    fun importSourceFailureRemovesPartialFile() = runTest {
        val store = AppPrivateAttachmentStore(attachments, staging)
        val failingSource = object : InputStream() {
            private var reads = 0

            override fun read(): Int = if (reads++ == 0) 1 else throw IOException("source lost")
        }

        expectThrows(AttachmentStorageException.StorageFailure::class.java) {
            store.import("note-1", "attachment-1", "file.bin") { failingSource }
        }

        assertFalse(File(attachments, "note-1/attachment-1.bin").exists())
        assertTrue(staging.listFiles().orEmpty().isEmpty())
    }

    private suspend fun <T : Throwable> expectThrows(
        type: Class<T>,
        block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (error: Throwable) {
            if (type.isInstance(error)) return requireNotNull(type.cast(error))
            throw AssertionError("Expected ${type.simpleName}, got ${error::class.java.simpleName}", error)
        }
        throw AssertionError("Expected ${type.simpleName} to be thrown")
    }
}
