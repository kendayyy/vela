package com.vela.notes.ui.scanner

import android.graphics.Bitmap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vela.notes.data.scanner.DocumentQuad
import com.vela.notes.data.scanner.ScanFilter
import com.vela.notes.data.scanner.ScanPoint
import com.vela.notes.ui.theme.VelaTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScanCornerEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun handlesResetConfirmFiltersAndFinish() {
        val image = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        val file = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "corner-editor-test.png",
        )
        FileOutputStream(file).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val initial = DocumentQuad(
            ScanPoint(8f, 6f),
            ScanPoint(70f, 6f),
            ScanPoint(70f, 52f),
            ScanPoint(8f, 52f),
        )
        var corrections = 0
        var lastFilter = ScanFilter.ORIGINAL
        var lastTurns = 0
        var finished: File? = null
        composeRule.setContent {
            VelaTheme {
                ScanCornerEditorScreen(
                    file = file,
                    quad = initial,
                    onClose = {},
                    onConfirmed = { finished = it },
                    correctPage = { _, _, turns, filter ->
                        corrections += 1
                        lastTurns = turns
                        lastFilter = filter
                        Bitmap.createBitmap(12, 10, Bitmap.Config.ARGB_8888)
                    },
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Góc 1 8,6").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Góc 1 8,6")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performTouchInput { swipeRight() }
        composeRule.onNodeWithContentDescription("Góc 1 8,6").assertDoesNotExist()
        composeRule.onNodeWithText("Đặt lại").performClick()
        composeRule.onNodeWithContentDescription("Góc 1 8,6").assertIsDisplayed()

        composeRule.onNodeWithText("Xác nhận").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Xong").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Xám").performClick()
        composeRule.waitUntil(5_000) { lastFilter == ScanFilter.GRAYSCALE }
        composeRule.onNodeWithContentDescription("Xoay phải").performClick()
        composeRule.waitUntil(5_000) {
            lastTurns == 1 &&
                composeRule.onAllNodesWithText("Đang chỉnh trang").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Xong").performClick()
        composeRule.waitUntil(5_000) { finished != null }

        val output = checkNotNull(finished)
        assertTrue(corrections >= 3)
        assertEquals(ScanFilter.GRAYSCALE, lastFilter)
        assertEquals(1, lastTurns)
        assertTrue(output.exists())
        assertTrue(output.length() > 0)
        output.delete()
        file.delete()
    }
}
