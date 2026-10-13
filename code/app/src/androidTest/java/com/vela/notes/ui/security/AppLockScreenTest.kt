package com.vela.notes.ui.security

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vela.notes.ui.theme.VelaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLockScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lockScreenHidesContentAndRequestsUnlock() {
        var clicks = 0
        composeRule.setContent {
            VelaTheme { AppLockScreen(onUnlock = { clicks += 1 }) }
        }

        composeRule.onNodeWithText("Vela đang được khóa").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mở khóa Vela").performClick()

        assertEquals(1, clicks)
    }
}
