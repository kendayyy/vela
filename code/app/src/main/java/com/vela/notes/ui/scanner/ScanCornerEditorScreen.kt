package com.vela.notes.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vela.notes.data.scanner.DocumentQuad
import com.vela.notes.data.scanner.ScanFilter
import com.vela.notes.data.scanner.ScanPageCorrector
import com.vela.notes.data.scanner.ScanPoint
import com.vela.notes.data.scanner.clampToImage
import com.vela.notes.data.scanner.defaultDocumentQuad
import com.vela.notes.data.scanner.fittedImageFrame
import com.vela.notes.data.scanner.magnifierSampleSquare
import com.vela.notes.data.scanner.toImagePoint
import com.vela.notes.data.scanner.toScreenPoint
import com.vela.notes.data.scanner.withCorner
import com.vela.notes.ui.theme.VelaAmber
import com.vela.notes.ui.theme.VelaSize
import com.vela.notes.ui.theme.VelaSpacing
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingScan(
    val file: File,
    val quad: DocumentQuad?,
)

@Composable
fun ScanCornerEditorScreen(
    file: File,
    quad: DocumentQuad?,
    onClose: () -> Unit,
    onConfirmed: (File) -> Unit,
    correctPage: (Bitmap, DocumentQuad, Int, ScanFilter) -> Bitmap = { bitmap, corners, turns, filter ->
        ScanPageCorrector().correct(bitmap, corners, turns, filter)
    },
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file) {
        val decoded = withContext(Dispatchers.Default) {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
            )
        }
        if (decoded == null) loadError = "Không thể mở ảnh quét." else image = decoded
    }
    when {
        loadError != null -> ScanMessage(loadError!!, onClose)
        image == null -> ScanMessage("Đang mở ảnh", onClose)
        else -> CornerEditor(
            image = image!!,
            initialQuad = quad.forImage(image!!.width, image!!.height),
            onClose = onClose,
            onConfirmed = onConfirmed,
            correctPage = correctPage,
        )
    }
}

@Composable
private fun CornerEditor(
    image: Bitmap,
    initialQuad: DocumentQuad,
    onClose: () -> Unit,
    onConfirmed: (File) -> Unit,
    correctPage: (Bitmap, DocumentQuad, Int, ScanFilter) -> Bitmap,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var corners by remember { mutableStateOf(initialQuad) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var confirmedQuad by remember { mutableStateOf<DocumentQuad?>(null) }
    var turns by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf(ScanFilter.ORIGINAL) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val reviewing = confirmedQuad != null
    val retiredPreviews = remember { mutableListOf<Bitmap>() }
    val latestPreview = rememberUpdatedState(preview)

    DisposableEffect(image) {
        onDispose {
            retiredPreviews.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
            latestPreview.value?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
            if (!image.isRecycled) image.recycle()
        }
    }
    LaunchedEffect(preview) {
        val current = preview ?: return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        val stale = retiredPreviews.filter { it !== current && !it.isRecycled }
        stale.forEach { it.recycle() }
        retiredPreviews.removeAll { it.isRecycled }
    }
    LaunchedEffect(confirmedQuad, turns, filter) {
        val active = confirmedQuad ?: return@LaunchedEffect
        busy = true
        error = null
        val result = runCatching {
            withContext(Dispatchers.Default) {
                correctPage(image, active, turns, filter)
            }
        }
        busy = false
        result.onSuccess { corrected ->
            preview?.let { retiredPreviews.add(it) }
            preview = corrected
        }.onFailure {
            error = "Không thể chỉnh trang. Hãy thử lại."
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(VelaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Đóng trình chỉnh góc", tint = Color.White)
            }
            Text(
                if (reviewing) "Chỉnh trang" else "Chỉnh góc",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(CORNER_EDITOR_IMAGE),
        ) {
            val shown = if (reviewing) preview ?: image else image
            Image(
                bitmap = shown.asImageBitmap(),
                contentDescription = "Trang quét",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (!reviewing) {
                CornerOverlay(
                    image = image,
                    corners = corners,
                    viewWidth = constraints.maxWidth.toFloat(),
                    viewHeight = constraints.maxHeight.toFloat(),
                    dragging = dragging,
                    onDragStart = { dragging = it },
                    onDragEnd = { dragging = null },
                    onMove = { index, point -> corners = corners.withCorner(index, point) },
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(VelaSpacing.Md)) }
        if (busy) {
            Row(
                Modifier.fillMaxWidth().padding(VelaSpacing.Md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), color = VelaAmber)
                Text("Đang chỉnh trang", color = Color.White, modifier = Modifier.padding(start = VelaSpacing.Sm))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(VelaSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!reviewing) {
                TextButton(onClick = { corners = initialQuad }, modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget)) {
                    Text("Đặt lại")
                }
                TextButton(
                    onClick = { confirmedQuad = corners },
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                ) { Text("Xác nhận") }
            } else {
                IconButton(onClick = { turns += 1 }) {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Xoay phải", tint = Color.White)
                }
                ScanFilter.entries.forEach { option ->
                    TextButton(
                        onClick = { filter = option },
                        modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                    ) {
                        Text(option.label(), color = if (option == filter) VelaAmber else Color.White)
                    }
                }
                TextButton(
                    onClick = {
                        val corrected = preview ?: return@TextButton
                        scope.launch {
                            val output = withContext(Dispatchers.Default) {
                                val file = File.createTempFile("vela-scan-page-", ".jpg", context.cacheDir)
                                FileOutputStream(file).use { stream ->
                                    corrected.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                                }
                                file
                            }
                            onConfirmed(output)
                        }
                    },
                    enabled = preview != null && !busy,
                    modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget),
                ) { Text("Xong") }
            }
        }
    }
}

@Composable
private fun BoxScope.CornerOverlay(
    image: Bitmap,
    corners: DocumentQuad,
    viewWidth: Float,
    viewHeight: Float,
    dragging: Int?,
    onDragStart: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, ScanPoint) -> Unit,
) {
    val density = LocalDensity.current
    val frame = fittedImageFrame(viewWidth, viewHeight, image.width, image.height)
    val handleRadius = with(density) { 24.dp.toPx() }
    val currentCorners by rememberUpdatedState(corners)
    if (dragging != null) {
        Magnifier(
            image = image,
            focus = currentCorners.corners[dragging],
            modifier = Modifier.align(Alignment.TopEnd).padding(VelaSpacing.Lg),
        )
    }
    corners.corners.forEachIndexed { index, point ->
        val screen = frame.toScreenPoint(point, image.width, image.height)
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (screen.x - handleRadius).roundToInt(),
                        (screen.y - handleRadius).roundToInt(),
                    )
                }
                .size(48.dp)
                .semantics { contentDescription = "Góc ${index + 1} ${point.x.toInt()},${point.y.toInt()}" }
                .pointerInput(index, frame) {
                    var dragged = currentCorners.corners[index]
                    detectDragGestures(
                        onDragStart = {
                            dragged = currentCorners.corners[index]
                            onDragStart(index)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                    ) { change, drag ->
                        change.consume()
                        val origin = frame.toScreenPoint(dragged, image.width, image.height)
                        dragged = frame.toImagePoint(
                            origin.x + drag.x,
                            origin.y + drag.y,
                            image.width,
                            image.height,
                        )
                        onMove(index, dragged)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(22.dp).background(VelaAmber, CircleShape))
        }
    }
}

@Composable
private fun Magnifier(image: Bitmap, focus: ScanPoint, modifier: Modifier = Modifier) {
    val sample = magnifierSampleSquare(focus, image.width, image.height, 48)
    Canvas(
        modifier
            .size(112.dp)
            .clip(CircleShape)
            .semantics { contentDescription = "Kính lúp" },
    ) {
        drawImage(
            image = image.asImageBitmap(),
            srcOffset = IntOffset(sample.left.toInt(), sample.top.toInt()),
            srcSize = IntSize(sample.width.toInt().coerceAtLeast(1), sample.height.toInt().coerceAtLeast(1)),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}

@Composable
private fun ScanMessage(message: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.White)
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = VelaSize.MinimumTouchTarget)) {
                Text("Đóng")
            }
        }
    }
}

private fun DocumentQuad?.forImage(width: Int, height: Int): DocumentQuad {
    val source = this ?: return defaultDocumentQuad(width, height)
    return DocumentQuad(
        topLeft = clampToImage(source.topLeft.x, source.topLeft.y, width, height),
        topRight = clampToImage(source.topRight.x, source.topRight.y, width, height),
        bottomRight = clampToImage(source.bottomRight.x, source.bottomRight.y, width, height),
        bottomLeft = clampToImage(source.bottomLeft.x, source.bottomLeft.y, width, height),
    )
}

private fun ScanFilter.label(): String = when (this) {
    ScanFilter.ORIGINAL -> "Gốc"
    ScanFilter.COLOR -> "Màu"
    ScanFilter.GRAYSCALE -> "Xám"
    ScanFilter.BLACK_AND_WHITE -> "Đen trắng"
}

const val CORNER_EDITOR_IMAGE = "corner_editor_image"
