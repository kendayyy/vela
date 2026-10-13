package com.vela.notes.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.vela.notes.ui.theme.VelaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VelaAppFrameTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capsuleIsPlacedBelowStatusBar() {
        var statusBarTop = 0.dp
        composeRule.setContent {
            val density = LocalDensity.current
            statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
            VelaTheme(darkTheme = false) {
                VelaAppFrame(focusState = FocusCapsuleState.Saving) {
                    Box(Modifier.fillMaxSize().testTag("content"))
                }
            }
        }

        composeRule.onNodeWithText("Đang lưu").assertIsDisplayed()
        val bounds = composeRule.onNodeWithTag(FOCUS_CAPSULE_TEST_TAG).getUnclippedBoundsInRoot()
        assertTrue(bounds.top >= statusBarTop)
    }

    @Test
    fun hiddenStateDoesNotAddCapsule() {
        composeRule.setContent {
            VelaTheme {
                VelaAppFrame(focusState = FocusCapsuleState.Hidden) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.onNodeWithTag(FOCUS_CAPSULE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun ocrProgressCanBeCancelledFromTheCapsule() {
        var cancelled = false
        composeRule.setContent {
            VelaTheme {
                VelaAppFrame(
                    focusState = FocusCapsuleState.Ocr(completed = 1, total = 2),
                    onCapsuleClick = { cancelled = true },
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.onNodeWithText("Đang nhận dạng 1/2").assertIsDisplayed()
        val capsule = composeRule.onNodeWithTag(FOCUS_CAPSULE_TEST_TAG).getUnclippedBoundsInRoot()
        val capsuleHeight = capsule.bottom - capsule.top
        val capsuleWidth = capsule.right - capsule.left
        assertTrue(
            "capsule ${capsuleWidth.value}x${capsuleHeight.value}",
            capsuleHeight.value >= 47.5f && capsuleWidth.value >= 47.5f,
        )
        composeRule.onNodeWithContentDescription("Hủy nhận dạng").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun capsuleLeavesContentBelowItsBounds() {
        composeRule.setContent {
            VelaTheme {
                VelaAppFrame(focusState = FocusCapsuleState.Saving) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .testTag("content"),
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val capsule = composeRule.onNodeWithTag(FOCUS_CAPSULE_TEST_TAG).getUnclippedBoundsInRoot()
        val content = composeRule.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertTrue(content.top + 1.dp >= capsule.bottom)
    }
}
