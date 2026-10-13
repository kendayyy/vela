package com.vela.notes.ui.sheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class VelaInsertSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersEveryActionWithAccessibleTouchTargets() {
        composeRule.setContent {
            VelaTheme {
                VelaInsertSheet(visible = true, onDismiss = {}, onAction = {})
            }
        }

        InsertAction.entries.forEach { action ->
            composeRule.onNodeWithContentDescription(action.label)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithContentDescription("Hủy thêm nội dung").assertIsDisplayed()
    }

    @Test
    fun selectingActionDispatchesAndDismisses() {
        var visible by mutableStateOf(true)
        var selected: InsertAction? = null
        composeRule.setContent {
            VelaTheme {
                VelaInsertSheet(
                    visible = visible,
                    onDismiss = { visible = false },
                    onAction = { selected = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Quét tài liệu").performClick()

        assertEquals(InsertAction.SCAN, selected)
        assertFalse(visible)
    }

    @Test
    fun cancelActionDismissesSheet() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            VelaTheme {
                VelaInsertSheet(
                    visible = visible,
                    onDismiss = { visible = false },
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Hủy thêm nội dung").performClick()
        composeRule.waitForIdle()

        assertFalse(visible)
        composeRule.onNodeWithText("Thêm vào ghi chú").assertDoesNotExist()
    }
}
