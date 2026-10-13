package com.vela.notes.ui.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

interface CameraSession {
    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun capture(rotation: Int, output: File, onSuccess: () -> Unit, onError: (Throwable) -> Unit)
    fun close()
}

class AndroidCameraSession(private val context: Context) : CameraSession {
    private val providerFuture = ProcessCameraProvider.getInstance(context)
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    override fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        providerFuture.addListener({
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
                provider = cameraProvider
                imageCapture = capture
            }
        }, executor)
    }

    override fun capture(
        rotation: Int,
        output: File,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val capture = imageCapture ?: return onError(IllegalStateException("Camera is not ready"))
        capture.targetRotation = rotation
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(output).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSuccess()
                override fun onError(exception: ImageCaptureException) = onError(exception)
            },
        )
    }

    override fun close() {
        provider?.unbindAll()
        provider = null
        imageCapture = null
    }
}

@Composable
fun CameraCaptureScreen(
    onClose: () -> Unit,
    onCaptured: (File) -> Unit,
    modifier: Modifier = Modifier,
    sessionFactory: (Context) -> CameraSession = ::AndroidCameraSession,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val session = remember { sessionFactory(context) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(session, lifecycleOwner) {
        session.bind(lifecycleOwner, previewView)
        onDispose { session.close() }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize().testTag(CAMERA_PREVIEW_TAG),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Đóng máy ảnh", tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (errorMessage != null) Text(errorMessage!!, color = Color.White)
            IconButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    errorMessage = null
                    val output = File.createTempFile("vela-camera-", ".jpg", context.cacheDir)
                    session.capture(
                        rotation = previewView.display?.rotation ?: Surface.ROTATION_0,
                        output = output,
                        onSuccess = { busy = false; onCaptured(output) },
                        onError = { error ->
                            output.delete()
                            busy = false
                            errorMessage = error.message ?: "Không thể chụp ảnh"
                        },
                    )
                },
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(6.dp)
                    .background(Color.White, CircleShape)
                    .testTag(CAMERA_SHUTTER_TAG),
            ) {
                Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = "Chụp ảnh",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

const val CAMERA_PREVIEW_TAG = "camera_preview"
const val CAMERA_SHUTTER_TAG = "camera_shutter"
