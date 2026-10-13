package com.vela.notes.ui.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.LifecycleOwner
import com.vela.notes.ui.theme.VelaTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraCaptureScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shutterCapturesToTemporaryFileAndCloseDispatches() {
        val session = FakeCameraSession()
        var captured: File? = null
        var closeCount = 0
        composeRule.setContent {
            VelaTheme {
                CameraCaptureScreen(
                    onClose = { closeCount += 1 },
                    onCaptured = { captured = it },
                    sessionFactory = { session },
                )
            }
        }

        composeRule.onNodeWithTag(CAMERA_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
        composeRule.waitForIdle()

        assertTrue(session.bound)
        assertTrue(captured?.exists() == true)
        composeRule.onNodeWithContentDescription("Đóng máy ảnh").performClick()
        assertEquals(1, closeCount)
        captured?.delete()
    }

    @Test
    fun captureFailureShowsRecoverableMessage() {
        val session = FakeCameraSession(failure = IllegalStateException("camera unavailable"))
        composeRule.setContent {
            VelaTheme {
                CameraCaptureScreen(
                    onClose = {},
                    onCaptured = {},
                    sessionFactory = { session },
                )
            }
        }

        composeRule.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()

        composeRule.onNodeWithText("camera unavailable").assertIsDisplayed()
    }

    private class FakeCameraSession(
        private val failure: Throwable? = null,
    ) : CameraSession {
        var bound = false

        override fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
            bound = true
        }

        override fun capture(
            rotation: Int,
            output: File,
            onSuccess: () -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            if (failure != null) {
                onError(failure)
            } else {
                output.writeBytes(byteArrayOf(1, 2, 3))
                onSuccess()
            }
        }

        override fun close() = Unit
    }
}
