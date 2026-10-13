package com.vela.notes.ui.scanner

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
import com.vela.notes.data.scanner.DocumentQuad
import com.vela.notes.data.scanner.ScanGuide
import com.vela.notes.data.scanner.ScanPhase
import com.vela.notes.data.scanner.ScanPoint
import com.vela.notes.ui.theme.VelaTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScannerPreviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun previewBindsAndCloseReleasesSession() {
        val session = FakeScannerSession()
        var closeCount = 0
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) {
                VelaTheme {
                    ScannerPreviewScreen(
                        onClose = { closeCount += 1 },
                        sessionFactory = { session },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(SCANNER_PREVIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Không thấy cạnh tài liệu").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Đang tìm cạnh tài liệu").assertIsDisplayed()
        assertTrue(session.bound)

        composeRule.onNodeWithContentDescription("Đóng máy quét").performClick()
        assertEquals(1, closeCount)

        visible = false
        composeRule.waitForIdle()
        assertTrue(session.closed)
    }

    @Test
    fun stableGuideShowsCountdownAndManualCaptureDispatches() {
        val session = FakeScannerSession()
        var captured: File? = null
        composeRule.setContent {
            VelaTheme {
                ScannerPreviewScreen(
                    onClose = {},
                    onCaptured = { file, _ ->
                        captured = file
                        true
                    },
                    sessionFactory = { session },
                )
            }
        }
        val quad = DocumentQuad(
            ScanPoint(20f, 20f),
            ScanPoint(80f, 24f),
            ScanPoint(76f, 90f),
            ScanPoint(22f, 88f),
        )

        composeRule.runOnIdle {
            session.emit(
                ScanGuide(
                    phase = ScanPhase.STABLE,
                    quad = quad,
                    imageWidth = 100,
                    imageHeight = 120,
                    countdown = 2,
                ),
            )
        }

        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tài liệu đã ổn định").assertIsDisplayed()
        composeRule.onNodeWithText("Giữ yên tài liệu").assertIsDisplayed()
        composeRule.onNodeWithTag(SCANNER_SHUTTER_TAG).performClick()

        assertEquals(1, session.manualCaptures)
        assertTrue(captured?.exists() == true)
        captured?.delete()
    }

    private class FakeScannerSession : ScannerSession {
        var bound = false
        var closed = false
        var manualCaptures = 0
        private var onGuide: (ScanGuide) -> Unit = {}
        private var onCaptured: (File, DocumentQuad?) -> Boolean = { _, _ -> false }

        override fun bind(
            lifecycleOwner: LifecycleOwner,
            previewView: PreviewView,
            onGuide: (ScanGuide) -> Unit,
            onCaptured: (File, DocumentQuad?) -> Boolean,
        ) {
            bound = true
            this.onGuide = onGuide
            this.onCaptured = onCaptured
            onGuide(ScanGuide.Searching)
        }

        override fun updateRotation(rotation: Int) = Unit

        override fun captureManual() {
            manualCaptures += 1
            val file = File.createTempFile("vela-scan-", ".jpg")
            file.writeBytes(byteArrayOf(1, 2, 3))
            onCaptured(file, null)
        }

        override fun close() {
            closed = true
        }

        fun emit(guide: ScanGuide) {
            onGuide(guide)
        }
    }
}
