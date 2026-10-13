package com.vela.notes.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.vela.notes.data.photo.PhotoImporter
import com.vela.notes.data.photo.PhotoSelectionReader
import com.vela.notes.data.file.DocumentSelectionReader
import com.vela.notes.data.file.FileImporter
import com.vela.notes.domain.model.Attachment
import com.vela.notes.domain.model.NoteBlock
import com.vela.notes.domain.model.NoteDocument
import com.vela.notes.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import java.io.File
import java.io.FileInputStream
import com.vela.notes.data.photo.PhotoSelection
import com.vela.notes.data.attachment.AttachmentSource
import com.vela.notes.data.attachment.AttachmentActionHandler
import com.vela.notes.data.attachment.AttachmentRemovalPlanner
import com.vela.notes.data.attachment.AttachmentStorageException
import com.vela.notes.ui.audio.RecorderEvent
import com.vela.notes.ui.audio.RecorderState
import com.vela.notes.ui.audio.reduce
import com.vela.notes.data.audio.AudioRecorderEngine
import com.vela.notes.data.audio.AudioImporter
import com.vela.notes.data.audio.AudioWaveformExtractor
import com.vela.notes.data.audio.AudioPlaybackController
import com.vela.notes.data.audio.AudioPlaybackState
import com.vela.notes.data.scanner.ScanOcr
import com.vela.notes.data.scanner.ScanPage
import com.vela.notes.data.scanner.ScanPdfImporter
import com.vela.notes.data.scanner.discardScanPages
import com.vela.notes.data.scanner.withAttachmentOcr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.vela.notes.core.time.VelaClock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.vela.notes.ui.audio.elapsedMillis

data class EditorUiState(
    val isLoading: Boolean = true,
    val editor: EditorState? = null,
    val errorMessage: String? = null,
    val isImporting: Boolean = false,
    val recorderState: RecorderState = RecorderState.Idle,
    val recordingElapsedSeconds: Long = 0,
    val playbackState: AudioPlaybackState = AudioPlaybackState(),
    val ocrCompleted: Int? = null,
    val ocrTotal: Int? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val photoSelectionReader: PhotoSelectionReader,
    private val photoImporter: PhotoImporter,
    private val documentSelectionReader: DocumentSelectionReader,
    private val fileImporter: FileImporter,
    private val attachmentActionHandler: AttachmentActionHandler,
    private val audioRecorderEngine: AudioRecorderEngine,
    private val audioImporter: AudioImporter,
    private val audioWaveformExtractor: AudioWaveformExtractor,
    private val audioPlaybackController: AudioPlaybackController,
    private val scanPdfImporter: ScanPdfImporter,
    private val scanOcr: ScanOcr,
    private val clock: VelaClock,
) : ViewModel() {
    private val noteId: String = checkNotNull(savedStateHandle[NOTE_ID_ARGUMENT])
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    private var sourceDocument: NoteDocument? = null
    private var recordingTicker: Job? = null
    private var scanJob: Job? = null
    private var hasPendingEdits = false
    private val autosave = EditorAutosaveCoordinator(
        scope = viewModelScope,
        save = repository::saveNote,
        onError = {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Không thể lưu thay đổi. Hãy thử lại.",
            )
        },
    )

    init {
        viewModelScope.launch {
            audioPlaybackController.state.collect { playbackState ->
                _uiState.value = _uiState.value.copy(playbackState = playbackState)
            }
        }
        viewModelScope.launch {
            val document = repository.observeNote(noteId).filterNotNull().first()
            sourceDocument = document
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                editor = EditorState.from(document, ::formatTimestamp),
            )
        }
    }

    fun toggleAudioPlayback(block: EditorMediaBlock) {
        val attachmentId = block.attachmentId ?: return
        val relativePath = block.relativePath ?: return
        audioPlaybackController.toggle(attachmentId, relativePath)
    }

    fun seekAudio(positionMs: Long) {
        audioPlaybackController.seekTo(positionMs)
    }

    fun setAudioSpeed(speed: Float) {
        audioPlaybackController.setSpeed(speed)
    }

    fun onAction(action: EditorAction) {
        val current = _uiState.value.editor ?: return
        val updated = current.reduce(action)
        _uiState.value = _uiState.value.copy(editor = updated, errorMessage = null)
        if (action is EditorAction.BlockFocused || updated == current) return

        val document = sourceDocument ?: return
        val snapshot = updated.toDocument(document)
        sourceDocument = snapshot
        hasPendingEdits = true
        autosave.submit(snapshot)
    }

    fun flushAndThen(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { autosave.flush() }
                .onSuccess { onComplete() }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Chưa thể lưu ghi chú. Vui lòng thử lại.",
                    )
                }
        }
    }

    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isImporting) return
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val selections = uris.map(photoSelectionReader::read)
                photoImporter.import(noteId, selections, startingPosition = 0)
            }.onSuccess { imported ->
                appendImportedMedia(imported.attachments, imported.blocks)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "Không thể thêm ảnh. Hãy kiểm tra tệp và thử lại.",
                )
            }
        }
    }

    fun importCameraPhoto(file: File) {
        if (_uiState.value.isImporting) {
            file.delete()
            return
        }
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val imported = photoImporter.import(
                    noteId = noteId,
                    selections = listOf(
                        PhotoSelection(
                            displayName = file.name,
                            mimeType = "image/jpeg",
                            source = AttachmentSource { FileInputStream(file) },
                        ),
                    ),
                    startingPosition = 0,
                )
                appendImportedMedia(imported.attachments, imported.blocks)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "Không thể lưu ảnh vừa chụp.",
                )
            } finally {
                file.delete()
            }
        }
    }

    fun onCameraPermissionDenied(forScan: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            errorMessage = if (forScan) {
                "Vela cần quyền máy ảnh để quét tài liệu."
            } else {
                "Vela cần quyền máy ảnh để chụp ảnh."
            },
        )
    }

    fun onRecordAudioRequested(permissionGranted: Boolean) {
        val recorder = _uiState.value.recorderState.reduce(
            RecorderEvent.StartRequested(permissionGranted),
        )
        _uiState.value = _uiState.value.copy(recorderState = recorder, errorMessage = null)
        if (recorder == RecorderState.Ready) startRecording()
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        val recorder = _uiState.value.recorderState.reduce(RecorderEvent.PermissionResult(granted))
        _uiState.value = _uiState.value.copy(
            recorderState = recorder,
            errorMessage = if (granted) null else "Vela cần quyền microphone để ghi âm.",
        )
        if (recorder == RecorderState.Ready) startRecording()
    }

    private fun startRecording() {
        if (_uiState.value.recorderState != RecorderState.Ready) return
        runCatching {
            audioRecorderEngine.start(onMaximumDurationReached = ::stopRecording)
            val recording = RecorderState.Ready.reduce(RecorderEvent.StartSucceeded(clock.nowMillis()))
            _uiState.value = _uiState.value.copy(
                recorderState = recording,
                recordingElapsedSeconds = 0,
                errorMessage = null,
            )
            startRecordingTicker()
        }.onFailure { recordingFailed("Không thể bắt đầu ghi âm.") }
    }

    fun pauseRecording() {
        val state = _uiState.value.recorderState
        if (state !is RecorderState.Recording) return
        runCatching { audioRecorderEngine.pause() }
            .onSuccess {
                val paused = state.reduce(RecorderEvent.PauseRequested(clock.nowMillis()))
                _uiState.value = _uiState.value.copy(
                    recorderState = paused,
                    recordingElapsedSeconds = paused.elapsedMillis(clock.nowMillis()) / 1_000,
                )
            }
            .onFailure { recordingFailed("Không thể tạm dừng ghi âm.") }
    }

    fun resumeRecording() {
        val state = _uiState.value.recorderState
        if (state !is RecorderState.Paused) return
        runCatching { audioRecorderEngine.resume() }
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    recorderState = state.reduce(RecorderEvent.ResumeSucceeded(clock.nowMillis())),
                )
            }
            .onFailure { recordingFailed("Không thể tiếp tục ghi âm.") }
    }

    fun stopRecording() {
        val state = _uiState.value.recorderState
        val stopping = state.reduce(RecorderEvent.StopRequested(clock.nowMillis()))
        if (stopping !is RecorderState.Stopping) return
        recordingTicker?.cancel()
        _uiState.value = _uiState.value.copy(
            recorderState = stopping,
            recordingElapsedSeconds = stopping.durationMillis / 1_000,
        )
        val file = runCatching { audioRecorderEngine.stop() }
            .getOrElse {
                recordingFailed("Bản ghi quá ngắn hoặc không hợp lệ.")
                return
            }
        viewModelScope.launch {
            try {
                val waveform = runCatching { audioWaveformExtractor.extract(file) }
                    .getOrDefault(byteArrayOf())
                val imported = audioImporter.import(
                    noteId = noteId,
                    file = file,
                    durationMillis = stopping.durationMillis,
                    waveform = waveform,
                )
                appendImportedMedia(listOf(imported.attachment), listOf(imported.block))
                _uiState.value = _uiState.value.copy(
                    recorderState = stopping.reduce(RecorderEvent.Stopped),
                    recordingElapsedSeconds = 0,
                )
            } catch (_: Exception) {
                recordingFailed("Không thể lưu bản ghi âm.")
            } finally {
                file.delete()
            }
        }
    }

    private fun recordingFailed(message: String) {
        recordingTicker?.cancel()
        audioRecorderEngine.cancel()
        _uiState.value = _uiState.value.copy(
            recorderState = _uiState.value.recorderState.reduce(RecorderEvent.Failed(message)),
            errorMessage = message,
        )
    }

    private fun startRecordingTicker() {
        recordingTicker?.cancel()
        recordingTicker = viewModelScope.launch {
            while (isActive) {
                val recorderState = _uiState.value.recorderState
                _uiState.value = _uiState.value.copy(
                    recordingElapsedSeconds = recorderState.elapsedMillis(clock.nowMillis()) / 1_000,
                )
                delay(RECORDING_TICK_MILLIS)
            }
        }
    }

    fun removeMedia(blockId: String) {
        val current = _uiState.value.editor ?: return
        val latest = sourceDocument ?: return
        val documentWithEdits = current.toDocument(latest)
        val plan = AttachmentRemovalPlanner.create(documentWithEdits, blockId) ?: return
        sourceDocument = plan.document
        _uiState.value = _uiState.value.copy(
            editor = current.copy(mediaBlocks = current.mediaBlocks.filterNot { it.id == blockId }),
            errorMessage = null,
        )
        autosave.submit(plan.document)
        viewModelScope.launch {
            runCatching {
                autosave.flush()
                attachmentActionHandler.deleteFile(plan.relativePathToDelete)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Đã xóa khỏi ghi chú; tệp sẽ được dọn dẹp sau.",
                )
            }
        }
    }

    fun openMediaIntent(
        context: android.content.Context,
        block: EditorMediaBlock,
    ): android.content.Intent? = block.relativePath?.let { path ->
        attachmentActionHandler.openIntent(context, path, block.mimeType ?: "application/octet-stream")
    }

    fun shareMediaIntent(
        context: android.content.Context,
        block: EditorMediaBlock,
    ): android.content.Intent? = block.relativePath?.let { path ->
        attachmentActionHandler.shareIntent(context, path, block.mimeType ?: "application/octet-stream")
    }

    fun importScan(pages: List<ScanPage>, workingDirectory: File, onFinished: (Boolean) -> Unit) {
        if (pages.isEmpty() || _uiState.value.isImporting) {
            onFinished(false)
            return
        }
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null, ocrCompleted = null, ocrTotal = null)
        scanJob = viewModelScope.launch {
            var importedId: String? = null
            try {
                val imported = scanPdfImporter.import(noteId, pages, workingDirectory)
                importedId = imported.attachment.id
                appendImportedMedia(listOf(imported.attachment), listOf(imported.block))
                onFinished(true)
                val text = scanOcr.recognize(pages) { completed, total ->
                    _uiState.value = _uiState.value.copy(ocrCompleted = completed, ocrTotal = total)
                }
                val document = sourceDocument ?: return@launch
                val updated = document.withAttachmentOcr(imported.attachment.id, text)
                sourceDocument = updated
                autosave.submit(updated)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = if (importedId == null) {
                        "Không thể lưu bản quét. Hãy thử lại."
                    } else {
                        "Đã lưu bản quét, nhưng chưa nhận dạng được chữ."
                    },
                )
                if (importedId == null) onFinished(false)
            } finally {
                if (importedId != null) discardScanPages(pages)
                _uiState.value = _uiState.value.copy(isImporting = false, ocrCompleted = null, ocrTotal = null)
            }
        }
    }

    fun cancelOcr() {
        scanJob?.cancel()
    }

    fun importFile(uri: Uri?) {
        if (uri == null || _uiState.value.isImporting) return
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                fileImporter.import(noteId, documentSelectionReader.read(uri))
            }.onSuccess { imported ->
                appendImportedMedia(listOf(imported.attachment), listOf(imported.block))
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = if (error is AttachmentStorageException.StorageFailure) {
                        "Không đủ dung lượng để lưu tệp."
                    } else {
                        "Không thể đính kèm tệp. Giới hạn tối đa là 50 MB."
                    },
                )
            }
        }
    }

    private fun appendImportedMedia(attachments: List<Attachment>, blocks: List<NoteBlock>) {
        val current = _uiState.value.editor ?: return
        val latest = sourceDocument ?: return
        val documentWithEdits = current.toDocument(latest)
        val firstPosition = (documentWithEdits.blocks.maxOfOrNull { it.position } ?: -1_024L) + 1_024L
        val positionedBlocks = blocks.mapIndexed { index, block ->
            block.copy(position = firstPosition + index * 1_024L)
        }
        val updatedDocument = documentWithEdits.copy(
            attachments = documentWithEdits.attachments + attachments,
            blocks = (documentWithEdits.blocks + positionedBlocks).sortedBy { it.position },
        )
        sourceDocument = updatedDocument
        val mediaState = EditorState.from(updatedDocument, ::formatTimestamp).mediaBlocks
        _uiState.value = _uiState.value.copy(
            editor = current.copy(mediaBlocks = mediaState),
            isImporting = false,
        )
        autosave.submit(updatedDocument)
    }

    override fun onCleared() {
        recordingTicker?.cancel()
        scanJob?.cancel()
        audioRecorderEngine.cancel()
        audioPlaybackController.release()
        val document = sourceDocument
        val savePending = hasPendingEdits
        autosave.close()
        if (savePending && document != null) {
            runBlocking(Dispatchers.IO) {
                runCatching { repository.saveNote(document) }
            }
        }
        super.onCleared()
    }
}

private const val RECORDING_TICK_MILLIS = 250L

private fun formatTimestamp(epochMillis: Long): String =
    "Đã sửa ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))}"

const val NOTE_ID_ARGUMENT = "noteId"
