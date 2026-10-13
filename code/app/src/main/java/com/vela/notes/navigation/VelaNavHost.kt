package com.vela.notes.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vela.notes.ui.collections.CollectionKind
import com.vela.notes.ui.collections.CollectionsScreen
import com.vela.notes.ui.collections.DefaultCollections
import com.vela.notes.ui.notes.NotesScreen
import com.vela.notes.ui.notes.NotesCollection
import com.vela.notes.ui.notes.NoteRowUi
import com.vela.notes.ui.notes.NotesViewModel
import com.vela.notes.ui.editor.EditorScreen
import com.vela.notes.ui.editor.EditorViewModel
import com.vela.notes.ui.editor.NOTE_ID_ARGUMENT
import com.vela.notes.ui.editor.EditorMediaAction
import com.vela.notes.ui.sheet.InsertAction
import com.vela.notes.ui.camera.CameraCaptureScreen
import com.vela.notes.data.scanner.SCAN_PAGE_LIMIT_MESSAGE
import com.vela.notes.data.scanner.ScanDraftResult
import com.vela.notes.data.scanner.ScanFilter
import com.vela.notes.data.scanner.ScanPage
import com.vela.notes.data.scanner.ScanThumbnailWriter
import com.vela.notes.data.scanner.addScanPage
import com.vela.notes.data.scanner.deleteScanPage
import com.vela.notes.data.scanner.discardScanPages
import com.vela.notes.data.scanner.moveScanPage
import com.vela.notes.data.scanner.updateScanPage
import com.vela.notes.ui.scanner.PendingScan
import com.vela.notes.ui.scanner.ScanCornerEditorScreen
import com.vela.notes.ui.scanner.ScanPageTrayScreen
import com.vela.notes.ui.scanner.ScannerPreviewScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vela.notes.ui.audio.RecorderState
import com.vela.notes.ui.shell.FocusCapsuleState
import com.vela.notes.data.preferences.AppLockTimeout
import com.vela.notes.security.AppLockUiState
import com.vela.notes.ui.security.AppLockSettingsScreen
import com.vela.notes.ui.security.canAuthenticateForAppLock

@Composable
fun VelaNavHost(
    appLockState: AppLockUiState,
    onAppLockEnabledChange: (Boolean) -> Unit,
    onAppLockDisable: () -> Unit,
    onAppLockTimeoutChange: (AppLockTimeout) -> Unit,
    onFocusStateChange: (FocusCapsuleState) -> Unit = {},
    onCapsuleClickChange: ((() -> Unit)?) -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = VelaDestination.AllNotes.route,
    ) {
        composable(VelaDestination.Folders.route) {
            CollectionsScreen(
                collections = DefaultCollections,
                folders = emptyList(),
                onCollectionClick = { kind ->
                    val destination = when (kind) {
                        CollectionKind.ALL -> VelaDestination.AllNotes
                        CollectionKind.PINNED -> VelaDestination.PinnedNotes
                        CollectionKind.DELETED -> VelaDestination.DeletedNotes
                    }
                    navController.navigate(destination.route)
                },
                onFolderClick = {},
            )
        }
        composable(VelaDestination.AllNotes.route) {
            NotesDestination(
                title = "Ghi chú",
                collection = NotesCollection.ALL,
                showBackButton = false,
                onBack = navController::popBackStack,
                onOpenNote = { navController.navigate(editorRoute(it)) },
                onCollectionSelected = { collection -> navController.openCollection(collection) },
                onOpenSettings = { navController.navigate(VelaDestination.Settings.route) },
            )
        }
        composable(VelaDestination.PinnedNotes.route) {
            NotesDestination(
                title = "Đã ghim",
                collection = NotesCollection.PINNED,
                onBack = navController::popBackStack,
                onOpenNote = { navController.navigate(editorRoute(it)) },
                onCollectionSelected = { collection -> navController.openCollection(collection) },
                onOpenSettings = { navController.navigate(VelaDestination.Settings.route) },
            )
        }
        composable(VelaDestination.DeletedNotes.route) {
            NotesDestination(
                title = "Đã xóa",
                collection = NotesCollection.DELETED,
                trash = true,
                onBack = navController::popBackStack,
                onOpenNote = {},
                onCollectionSelected = { collection -> navController.openCollection(collection) },
                onOpenSettings = { navController.navigate(VelaDestination.Settings.route) },
            )
        }
        composable(VelaDestination.Settings.route) {
            val activity = LocalActivity.current as? FragmentActivity
            AppLockSettingsScreen(
                settings = appLockState.settings,
                isAuthenticationAvailable = activity?.let(::canAuthenticateForAppLock) == true,
                onBack = navController::popBackStack,
                onEnable = { onAppLockEnabledChange(true) },
                onDisable = onAppLockDisable,
                onTimeoutChange = onAppLockTimeoutChange,
            )
        }
        composable(
            route = VelaDestination.Editor.route,
            arguments = listOf(navArgument(NOTE_ID_ARGUMENT) { type = NavType.StringType }),
        ) {
            val viewModel: EditorViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            var showCamera by remember { mutableStateOf(false) }
            var showScanner by remember { mutableStateOf(false) }
            var pendingScan by remember { mutableStateOf<PendingScan?>(null) }
            var scanPages by remember { mutableStateOf<List<ScanPage>>(emptyList()) }
            var showScanTray by remember { mutableStateOf(false) }
            var scanLimitMessage by remember { mutableStateOf<String?>(null) }
            var confirmDiscardScan by remember { mutableStateOf(false) }
            val scanScope = rememberCoroutineScope()
            var pendingCapture by remember { mutableStateOf(InsertAction.CAMERA) }
            val cameraPermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    if (!granted) {
                        viewModel.onCameraPermissionDenied(forScan = pendingCapture == InsertAction.SCAN)
                        return@rememberLauncherForActivityResult
                    }
                    if (pendingCapture == InsertAction.SCAN) showScanner = true else showCamera = true
                },
            )
            val microphonePermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = viewModel::onMicrophonePermissionResult,
            )
            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
                onResult = viewModel::importPhotos,
            )
            val documentPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = viewModel::importFile,
            )
            val ocrCompleted = state.ocrCompleted
            val ocrTotal = state.ocrTotal
            val capsuleState = when {
                state.recorderState is RecorderState.Recording -> FocusCapsuleState.Recording(
                    elapsedSeconds = state.recordingElapsedSeconds,
                    isPaused = false,
                )
                state.recorderState is RecorderState.Paused -> FocusCapsuleState.Recording(
                    elapsedSeconds = state.recordingElapsedSeconds,
                    isPaused = true,
                )
                state.recorderState is RecorderState.Stopping -> FocusCapsuleState.Saving
                ocrCompleted != null && ocrTotal != null -> FocusCapsuleState.Ocr(ocrCompleted, ocrTotal)
                else -> FocusCapsuleState.Hidden
            }
            LaunchedEffect(capsuleState) { onFocusStateChange(capsuleState) }
            LaunchedEffect(ocrTotal) {
                onCapsuleClickChange(if (ocrTotal != null) viewModel::cancelOcr else null)
            }
            DisposableEffect(Unit) {
                onDispose { onFocusStateChange(FocusCapsuleState.Hidden) }
            }
            val editor = state.editor
            val scan = pendingScan
            when {
                scan != null -> ScanCornerEditorScreen(
                    file = scan.file,
                    quad = scan.quad,
                    onClose = {
                        scan.file.delete()
                        pendingScan = null
                        if (scanPages.isNotEmpty()) showScanTray = true
                    },
                    onConfirmed = { corrected ->
                        val currentPages = scanPages
                        scanScope.launch {
                            val thumbnail = withContext(Dispatchers.Default) {
                                val file = File.createTempFile("vela-scan-thumb-", ".jpg", corrected.parentFile)
                                val written = runCatching { ScanThumbnailWriter.write(corrected, file) }
                                if (written.isSuccess) {
                                    file
                                } else {
                                    file.delete()
                                    corrected
                                }
                            }
                            scan.file.delete()
                            when (val result = addScanPage(
                                currentPages,
                                ScanPage(
                                    id = corrected.name,
                                    imageFile = corrected,
                                    thumbnailFile = thumbnail,
                                ),
                            )) {
                                is ScanDraftResult.Ready -> {
                                    scanPages = result.pages
                                    scanLimitMessage = null
                                    pendingScan = null
                                    showScanTray = true
                                }
                                ScanDraftResult.LimitReached -> {
                                    corrected.delete()
                                    if (thumbnail != corrected) thumbnail.delete()
                                    scanLimitMessage = SCAN_PAGE_LIMIT_MESSAGE
                                    pendingScan = null
                                    showScanTray = true
                                }
                                ScanDraftResult.ConfirmDiscardLast -> pendingScan = null
                            }
                        }
                    },
                )
                showScanner -> ScannerPreviewScreen(
                    onClose = {
                        showScanner = false
                        if (scanPages.isNotEmpty()) showScanTray = true
                    },
                    onCaptured = { file, quad ->
                        showScanner = false
                        pendingScan = PendingScan(file, quad)
                        true
                    },
                )
                showScanTray -> ScanPageTrayScreen(
                    pages = scanPages,
                    limitMessage = scanLimitMessage,
                    confirmDiscardLast = confirmDiscardScan,
                    onAddPage = {
                        if (scanPages.size >= com.vela.notes.data.scanner.MAX_SCAN_PAGES) {
                            scanLimitMessage = SCAN_PAGE_LIMIT_MESSAGE
                        } else {
                            scanLimitMessage = null
                            showScanTray = false
                            showScanner = true
                        }
                    },
                    onMove = { from, to -> scanPages = moveScanPage(scanPages, from, to) },
                    onDelete = { id ->
                        when (val result = deleteScanPage(scanPages, id)) {
                            is ScanDraftResult.Ready -> scanPages = result.pages
                            ScanDraftResult.ConfirmDiscardLast -> confirmDiscardScan = true
                            ScanDraftResult.LimitReached -> Unit
                        }
                    },
                    onRotate = { id ->
                        scanPages = updateScanPage(scanPages, id) { page ->
                            page.copy(quarterTurns = page.quarterTurns + 1)
                        }
                    },
                    onFilter = { id, filter: ScanFilter ->
                        scanPages = updateScanPage(scanPages, id) { page -> page.copy(filter = filter) }
                    },
                    onConfirmDiscard = {
                        discardScanPages(scanPages)
                        scanPages = emptyList()
                        confirmDiscardScan = false
                        showScanTray = false
                        scanLimitMessage = null
                    },
                    onDismissDiscard = { confirmDiscardScan = false },
                    onDone = {
                        viewModel.importScan(scanPages, context.cacheDir) { saved ->
                            if (saved) {
                                scanPages = emptyList()
                                showScanTray = false
                                scanLimitMessage = null
                            }
                        }
                    },
                    onClose = {
                        discardScanPages(scanPages)
                        scanPages = emptyList()
                        showScanTray = false
                        scanLimitMessage = null
                    },
                )
                showCamera -> CameraCaptureScreen(
                    onClose = { showCamera = false },
                    onCaptured = { file ->
                        showCamera = false
                        viewModel.importCameraPhoto(file)
                    },
                )
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                editor != null -> EditorScreen(
                    state = editor,
                    onAction = viewModel::onAction,
                    onBack = { viewModel.flushAndThen(navController::popBackStack) },
                    errorMessage = state.errorMessage,
                    recorderState = state.recorderState,
                    recordingElapsedSeconds = state.recordingElapsedSeconds,
                    playbackState = state.playbackState,
                    onToggleAudio = viewModel::toggleAudioPlayback,
                    onSeekAudio = viewModel::seekAudio,
                    onAudioSpeedChange = viewModel::setAudioSpeed,
                    onPauseRecording = viewModel::pauseRecording,
                    onResumeRecording = viewModel::resumeRecording,
                    onStopRecording = viewModel::stopRecording,
                    onInsertAction = { action ->
                        if (action == InsertAction.PHOTOS) {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        } else if (action == InsertAction.ATTACH_FILE) {
                            documentPicker.launch(arrayOf("*/*"))
                        } else if (action == InsertAction.CAMERA || action == InsertAction.SCAN) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                if (action == InsertAction.SCAN) showScanner = true else showCamera = true
                            } else {
                                pendingCapture = action
                                cameraPermission.launch(Manifest.permission.CAMERA)
                            }
                        } else if (action == InsertAction.RECORD_AUDIO) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            viewModel.onRecordAudioRequested(granted)
                            if (!granted) microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onMediaAction = { block, action ->
                        when (action) {
                            EditorMediaAction.OPEN -> viewModel.openMediaIntent(context, block)?.let { intent ->
                                runCatching { context.startActivity(intent) }
                            }
                            EditorMediaAction.SHARE -> viewModel.shareMediaIntent(context, block)?.let { intent ->
                                runCatching {
                                    context.startActivity(Intent.createChooser(intent, "Chia sẻ tệp"))
                                }
                            }
                            EditorMediaAction.REMOVE -> viewModel.removeMedia(block.id)
                        }
                    },
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy ghi chú")
                }
            }
        }
    }
}

@Composable
private fun NotesDestination(
    title: String,
    collection: NotesCollection,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCollectionSelected: (NotesCollection) -> Unit,
    onOpenSettings: () -> Unit = {},
    trash: Boolean = false,
    showBackButton: Boolean = true,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val baseNotes by if (trash) {
        viewModel.deletedNotes.collectAsStateWithLifecycle()
    } else {
        viewModel.notes.collectAsStateWithLifecycle()
    }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val sourceNotes = if (!trash && query.isNotBlank()) searchResults else baseNotes
    val notes = if (collection == NotesCollection.PINNED) sourceNotes.filter(NoteRowUi::isPinned) else sourceNotes
    NotesScreen(
        title = title,
        notes = notes,
        query = query,
        onQueryChange = {
            query = it
            viewModel.updateSearchQuery(it)
        },
        onBack = onBack,
        onNoteClick = onOpenNote,
        onArchive = {},
        onDelete = viewModel::deleteNote,
        onCreateNote = { viewModel.createNote(onOpenNote) },
        onUndo = viewModel::restoreNote,
        isTrash = trash,
        onRestore = viewModel::restoreNote,
        onDeletePermanently = viewModel::deleteNotePermanently,
        onEmptyTrash = viewModel::emptyTrash,
        selectedCollection = collection,
        showBackButton = showBackButton,
        onCollectionSelected = onCollectionSelected,
        onOpenSettings = onOpenSettings,
        searchAlreadyApplied = !trash && query.isNotBlank(),
    )
}

private fun androidx.navigation.NavHostController.openCollection(collection: NotesCollection) {
    val destination = when (collection) {
        NotesCollection.ALL -> VelaDestination.AllNotes
        NotesCollection.PINNED -> VelaDestination.PinnedNotes
        NotesCollection.FOLDERS -> VelaDestination.Folders
        NotesCollection.DELETED -> VelaDestination.DeletedNotes
    }
    if (destination == VelaDestination.AllNotes) {
        popBackStack(VelaDestination.AllNotes.route, inclusive = false)
    } else {
        navigate(destination.route) {
            popUpTo(VelaDestination.AllNotes.route) { inclusive = false }
            launchSingleTop = true
        }
    }
}
