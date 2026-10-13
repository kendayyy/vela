package com.vela.notes.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vela.notes.data.scanner.DocumentEdgeDetector
import com.vela.notes.data.scanner.DocumentQuad
import com.vela.notes.data.scanner.ScanCapturePlanner
import com.vela.notes.data.scanner.ScanGuide
import com.vela.notes.data.scanner.ScanPhase
import com.vela.notes.data.scanner.ScannerFrameAnalyzer
import com.vela.notes.ui.theme.VelaAmber
import com.vela.notes.ui.theme.VelaTeal
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

interface ScannerSession {
    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onGuide: (ScanGuide) -> Unit,
        onCaptured: (File, DocumentQuad?) -> Boolean,
    )

    fun updateRotation(rotation: Int)
    fun captureManual()
    fun close()
}

class AndroidScannerSession(context: Context) : ScannerSession {
    private val cacheDir = context.cacheDir
    private val providerFuture = ProcessCameraProvider.getInstance(context)
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "vela-scanner-analysis")
    }
    private val detector = DocumentEdgeDetector()
    private val planner = ScanCapturePlanner()
    private val closed = AtomicBoolean(false)
    private val frameLock = Any()
    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var targetRotation = Surface.ROTATION_0
    private var latest: Bitmap? = null
    private var latestQuad: DocumentQuad? = null
    private var latestWidth = 0
    private var latestHeight = 0
    private var onGuide: (ScanGuide) -> Unit = {}
    private var onCaptured: (File, DocumentQuad?) -> Boolean = { _, _ -> false }

    override fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onGuide: (ScanGuide) -> Unit,
        onCaptured: (File, DocumentQuad?) -> Boolean,
    ) {
        this.onGuide = onGuide
        this.onCaptured = onCaptured
        providerFuture.addListener({
            if (closed.get()) return@addListener
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(targetRotation)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                ),
                            )
                            .build(),
                    )
                    .build()
                imageAnalysis.setAnalyzer(
                    analysisExecutor,
                    ScannerFrameAnalyzer { image ->
                        val bitmap = image.toBitmap()
                        try {
                            val quad = detector.detect(bitmap)
                            val decision = planner.onFrame(quad, bitmap.width, bitmap.height)
                            replaceLatest(bitmap, decision.guide.quad)
                            mainExecutor.execute { onGuide(decision.guide) }
                            if (decision.shouldCapture) writeCapture()
                        } finally {
                            bitmap.recycle()
                        }
                    },
                )
                cameraProvider.unbindAll()
                if (closed.get()) return@runCatching
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
                if (closed.get()) {
                    cameraProvider.unbindAll()
                } else {
                    provider = cameraProvider
                    analysis = imageAnalysis
                }
            }
        }, mainExecutor)
    }

    override fun updateRotation(rotation: Int) {
        targetRotation = rotation
        mainExecutor.execute { analysis?.targetRotation = rotation }
    }

    override fun captureManual() {
        if (closed.get() || analysisExecutor.isShutdown) return
        analysisExecutor.execute {
            val decision = planner.captureManually(latestQuad, latestWidth, latestHeight)
            mainExecutor.execute { onGuide(decision.guide) }
            if (decision.shouldCapture) writeCapture()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        provider?.unbindAll()
        provider = null
        analysis = null
        synchronized(frameLock) {
            latest?.recycle()
            latest = null
        }
        analysisExecutor.shutdown()
    }

    private fun replaceLatest(bitmap: Bitmap, quad: DocumentQuad?) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        synchronized(frameLock) {
            if (closed.get()) {
                copy.recycle()
                return
            }
            latest?.recycle()
            latest = copy
            latestQuad = quad
            latestWidth = bitmap.width
            latestHeight = bitmap.height
        }
    }

    private fun writeCapture() {
        val snapshot = synchronized(frameLock) {
            latest?.copy(Bitmap.Config.ARGB_8888, false)?.let { it to latestQuad }
        } ?: return
        val (bitmap, quad) = snapshot
        try {
            val file = File.createTempFile("vela-scan-", ".jpg", cacheDir)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            mainExecutor.execute { onCaptured(file, quad) }
        } finally {
            bitmap.recycle()
        }
    }
}

@Composable
fun ScannerPreviewScreen(
    onClose: () -> Unit,
    onCaptured: (File, DocumentQuad?) -> Boolean = { _, _ -> false },
    modifier: Modifier = Modifier,
    sessionFactory: (Context) -> ScannerSession = ::AndroidScannerSession,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val session = remember { sessionFactory(context) }
    val ownedCaptures = remember { mutableListOf<File>() }
    var guide by remember { mutableStateOf(ScanGuide.Searching) }

    DisposableEffect(session, lifecycleOwner) {
        session.bind(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            onGuide = { guide = it },
            onCaptured = { file, quad ->
                ownedCaptures += file
                if (onCaptured(file, quad)) ownedCaptures.remove(file)
                true
            },
        )
        onDispose {
            session.close()
            ownedCaptures.forEach { it.delete() }
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            update = { view ->
                view.display?.rotation?.let(session::updateRotation)
            },
            modifier = Modifier.fillMaxSize().testTag(SCANNER_PREVIEW_TAG),
        )
        DocumentGuide(guide)
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Đóng máy quét", tint = Color.White)
        }
        Text(
            guide.statusLabel(),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 112.dp),
        )
        guide.countdown?.let { count ->
            Text(
                count.toString(),
                color = VelaAmber,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { contentDescription = "Đếm ngược $count" },
            )
        }
        IconButton(
            onClick = session::captureManual,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
                .testTag(SCANNER_SHUTTER_TAG),
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = "Chụp tài liệu", tint = Color.Black)
        }
    }
}

@Composable
private fun DocumentGuide(guide: ScanGuide) {
    val quad = guide.quad
    val searching = guide.phase == ScanPhase.SEARCHING || guide.phase == ScanPhase.TRACKING
    val color = if (searching) VelaTeal else VelaAmber
    val description = when (guide.phase) {
        ScanPhase.SEARCHING -> "Đang tìm cạnh tài liệu"
        ScanPhase.TRACKING -> "Đang theo cạnh tài liệu"
        ScanPhase.STABLE -> "Tài liệu đã ổn định"
        ScanPhase.CAPTURED -> "Đã chụp trang"
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        if (quad == null || guide.imageWidth <= 0 || guide.imageHeight <= 0) {
            val inset = 48.dp.toPx()
            drawRoundRect(
                color = VelaTeal,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - inset * 2,
                    size.height - inset * 2,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx()),
            )
            return@Canvas
        }
        val path = Path()
        quad.corners.forEachIndexed { index, point ->
            val offset = Offset(
                point.x / guide.imageWidth * size.width,
                point.y / guide.imageHeight * size.height,
            )
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        path.close()
        drawPath(path, color = color, style = Stroke(width = 4.dp.toPx()))
    }
}

private fun ScanGuide.statusLabel(): String = when (phase) {
    ScanPhase.SEARCHING -> "Không thấy cạnh tài liệu"
    ScanPhase.TRACKING -> "Đang tìm cạnh"
    ScanPhase.STABLE -> "Giữ yên tài liệu"
    ScanPhase.CAPTURED -> "Đã chụp trang"
}

const val SCANNER_PREVIEW_TAG = "scanner_preview"
const val SCANNER_SHUTTER_TAG = "scanner_shutter"
