package com.vela.notes.ui.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vela.notes.data.scanner.SCAN_PAGE_LIMIT_MESSAGE
import com.vela.notes.data.scanner.ScanFilter
import com.vela.notes.data.scanner.ScanPage
import com.vela.notes.data.scanner.moveScanPage
import com.vela.notes.data.scanner.updateScanPage
import com.vela.notes.ui.theme.VelaTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScanPageTrayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reorderFilterAndLastPageConfirmation() {
        val first = ScanPage("a", File("a.jpg"), File("a-thumb.jpg"))
        val second = ScanPage("b", File("b.jpg"), File("b-thumb.jpg"))
        var pages by mutableStateOf(listOf(first, second))
        var confirm by mutableStateOf(false)
        var discarded = false
        composeRule.setContent {
            VelaTheme {
                ScanPageTrayScreen(
                    pages = pages,
                    limitMessage = null,
                    confirmDiscardLast = confirm,
                    onAddPage = {},
                    onMove = { from, to -> pages = moveScanPage(pages, from, to) },
                    onDelete = { confirm = true },
                    onRotate = { id ->
                        pages = updateScanPage(pages, id) { page -> page.copy(quarterTurns = page.quarterTurns + 1) }
                    },
                    onFilter = { id, filter ->
                        pages = updateScanPage(pages, id) { page -> page.copy(filter = filter) }
                    },
                    onConfirmDiscard = { discarded = true },
                    onDismissDiscard = { confirm = false },
                    onDone = {},
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("Sang phải").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("b", "a"), pages.map { it.id })
        composeRule.onNodeWithText("Xám").performClick()
        composeRule.waitForIdle()
        assertEquals(ScanFilter.GRAYSCALE, pages.first { it.id == "b" }.filter)
        composeRule.onNodeWithText("Xóa trang").performClick()
        composeRule.onNodeWithText("Xóa trang cuối?").assertIsDisplayed()
        composeRule.onNodeWithText("Xóa").performClick()
        composeRule.waitForIdle()
        assertEquals(true, discarded)
    }

    @Test
    fun showsTheFiftyPageLimit() {
        composeRule.setContent {
            VelaTheme {
                ScanPageTrayScreen(
                    pages = listOf(ScanPage("a", File("a.jpg"), File("a.jpg"))),
                    limitMessage = SCAN_PAGE_LIMIT_MESSAGE,
                    confirmDiscardLast = false,
                    onAddPage = {},
                    onMove = { _, _ -> },
                    onDelete = {},
                    onRotate = {},
                    onFilter = { _, _ -> },
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onDone = {},
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText(SCAN_PAGE_LIMIT_MESSAGE).assertIsDisplayed()
    }
}
