package com.vela.notes.ui.editor

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.vela.notes.core.id.IdGenerator
import com.vela.notes.core.time.VelaClock
import com.vela.notes.data.attachment.AttachmentActionHandler
import com.vela.notes.data.attachment.AttachmentCleanupResult
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentStorageException
import com.vela.notes.data.attachment.AttachmentStore
import com.vela.notes.data.attachment.StoredAttachment
import com.vela.notes.data.audio.AudioImporter
import com.vela.notes.data.audio.AudioPlaybackController
import com.vela.notes.data.audio.AudioPlaybackState
import com.vela.notes.data.audio.AudioRecorderEngine
import com.vela.notes.data.audio.AudioWaveformExtractor
import com.vela.notes.data.file.DocumentSelectionReader
import com.vela.notes.data.file.FileImporter
import com.vela.notes.data.photo.ImageNormalizer
import com.vela.notes.data.photo.PhotoImporter
import com.vela.notes.data.photo.PhotoSelectionReader
import com.vela.notes.data.scanner.ScanDocumentExporter
import com.vela.notes.data.scanner.ScanOcr
import com.vela.notes.data.scanner.ScanPdfImporter
import com.vela.notes.domain.model.Note
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteBlockType
import com.vela.notes.domain.model.NoteDocument
import com.vela.notes.domain.repository.NoteRepository
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorResilienceTest {
    private val repository = MemoryNoteRepository(sampleDocument())
    private val recorder = FakeRecorder()
    private val playback = FakePlayback()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun clearingTheEditorPersistsTextThatHasNotDebouncedYet() {
        val viewModel = viewModel()
        val blockId = viewModel.uiState.value.editor!!.textBlocks.single().id

        viewModel.onAction(EditorAction.TextChanged(blockId, TextFieldValue("Hop luc 9 gio")))
        clear(viewModel)

        assertEquals("Hop luc 9 gio", repository.saved?.blocks?.single()?.textContent)
        assertTrue(recorder.cancelled)
        assertTrue(playback.released)
    }

    @Test
    fun permissionDenialKeepsTheEditorOpen() {
        val viewModel = viewModel()

        viewModel.onCameraPermissionDenied()
        viewModel.onMicrophonePermissionResult(granted = false)

        assertNotNull(viewModel.uiState.value.editor)
        assertEquals("Vela cần quyền microphone để ghi âm.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun badProviderUriAndLowStorageDoNotReplaceTheNote() {
        val brokenReader = DocumentSelectionReader { throw FileNotFoundException("bad uri") }
        val viewModel = viewModel(documentReader = brokenReader)

        viewModel.importFile(null)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.importFile(Uri.parse("content://vela/missing"))
        assertEquals("Không thể đính kèm tệp. Giới hạn tối đa là 50 MB.", viewModel.uiState.value.errorMessage)
        assertEquals("Nhap", viewModel.uiState.value.editor!!.textBlocks.single().text)

        val fullDisk = viewModel(store = FailingAttachmentStore())
        fullDisk.importFile(Uri.parse("content://vela/file"))
        assertEquals("Không đủ dung lượng để lưu tệp.", fullDisk.uiState.value.errorMessage)
        assertEquals("Nhap", fullDisk.uiState.value.editor!!.textBlocks.single().text)
        clear(viewModel)
        clear(fullDisk)
    }

    private fun viewModel(
        documentReader: DocumentSelectionReader = DocumentSelectionReader {
            com.vela.notes.data.file.DocumentSelection(
                displayName = "a.txt",
                mimeType = "text/plain",
                declaredSizeBytes = 1,
                source = { java.io.ByteArrayInputStream(byteArrayOf(1)) },
            )
        },
        store: AttachmentStore = UnusedAttachmentStore(),
    ): EditorViewModel {
        val clock = VelaClock { 1_000L }
        val ids = IdGenerator { "generated-id" }
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf(NOTE_ID_ARGUMENT to "note-1")),
            repository = repository,
            photoSelectionReader = PhotoSelectionReader { error("unused") },
            photoImporter = PhotoImporter(store, ids, clock, ImageNormalizer { error("unused") }),
            documentSelectionReader = documentReader,
            fileImporter = FileImporter(store, ids, clock),
            attachmentActionHandler = AttachmentActionHandler(store),
            audioRecorderEngine = recorder,
            audioImporter = AudioImporter(store, ids, clock),
            audioWaveformExtractor = object : AudioWaveformExtractor {
                override suspend fun extract(file: File, peakCount: Int) = byteArrayOf()
            },
            audioPlaybackController = playback,
            scanPdfImporter = ScanPdfImporter(store, ids, clock, ScanDocumentExporter { _, _ -> }),
            scanOcr = ScanOcr { _, _ -> null },
            clock = clock,
        )
    }

    private fun clear(viewModel: EditorViewModel) {
        ViewModelStore().apply {
            put("editor", viewModel)
            clear()
        }
    }

    private fun sampleDocument() = NoteDocument(
        note = Note(id = "note-1", title = "Nhap", createdAt = 1, updatedAt = 1),
        blocks = listOf(
            NoteBlock(
                id = "block-1",
                noteId = "note-1",
                type = NoteBlockType.TEXT,
                position = 0,
                textContent = "Nhap",
                createdAt = 1,
                updatedAt = 1,
            ),
        ),
        attachments = emptyList(),
    )
}

private class MemoryNoteRepository(initial: NoteDocument) : NoteRepository {
    var saved: NoteDocument? = null
    private val current = MutableStateFlow(initial)

    override fun observeActiveNotes() = flowOf(emptyList<com.vela.notes.domain.model.Note>())
    override fun observeDeletedNotes() = flowOf(emptyList<com.vela.notes.domain.model.Note>())
    override fun observeActiveNoteSummaries() = flowOf(emptyList<com.vela.notes.domain.model.NoteSummary>())
    override fun observeDeletedNoteSummaries() = flowOf(emptyList<com.vela.notes.domain.model.NoteSummary>())
    override fun observeNote(noteId: String): Flow<NoteDocument?> = current
    override suspend fun createNote() = error("unused")
    override suspend fun saveNote(document: NoteDocument) {
        saved = document
        current.value = document
    }
    override suspend fun searchNotes(query: String) = emptyList<com.vela.notes.domain.model.Note>()
    override suspend fun searchNoteSummaries(query: String) = emptyList<com.vela.notes.domain.model.NoteSummary>()
    override suspend fun softDelete(noteId: String) = Unit
    override suspend fun restore(noteId: String) = Unit
    override suspend fun deletePermanently(noteId: String) = Unit
    override suspend fun emptyTrash() = Unit
}

private class FakeRecorder : AudioRecorderEngine {
    var cancelled = false
    override fun start(onMaximumDurationReached: () -> Unit) = error("unused")
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = error("unused")
    override fun cancel() {
        cancelled = true
    }
    override fun cleanupStaleFiles() = 0
}

private class FakePlayback : AudioPlaybackController {
    var released = false
    override val state: StateFlow<AudioPlaybackState> = MutableStateFlow(AudioPlaybackState())
    override fun toggle(attachmentId: String, relativePath: String) = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun release() {
        released = true
    }
}

private open class UnusedAttachmentStore : AttachmentStore {
    override suspend fun import(
        noteId: String,
        attachmentId: String,
        displayName: String,
        source: AttachmentSource,
    ): StoredAttachment = error("unused")

    override suspend fun delete(relativePath: String) = Unit
    override suspend fun cleanup(referencedPaths: Set<String>, stagingOlderThanMillis: Long) =
        AttachmentCleanupResult(0, 0)
    override fun resolve(relativePath: String) = File(relativePath)
}

private class FailingAttachmentStore : UnusedAttachmentStore() {
    override suspend fun import(
        noteId: String,
        attachmentId: String,
        displayName: String,
        source: AttachmentSource,
    ): StoredAttachment = throw AttachmentStorageException.StorageFailure(IOException("No space left on device"))
}
