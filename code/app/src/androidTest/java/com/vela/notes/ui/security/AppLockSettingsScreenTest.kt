package com.vela.notes.ui.security

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vela.notes.data.preferences.AppLockSettings
import com.vela.notes.data.preferences.AppLockTimeout
import com.vela.notes.ui.theme.VelaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLockSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enablingAppLockDispatchesOnlyWhenAuthenticationIsAvailable() {
        var enableCalls = 0
        composeRule.setContent {
            VelaTheme {
                AppLockSettingsScreen(
                    settings = AppLockSettings(),
                    isAuthenticationAvailable = true,
                    onBack = {},
                    onEnable = { enableCalls += 1 },
                    onDisable = {},
                    onTimeoutChange = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Bật khóa ứng dụng").performClick()

        assertEquals(1, enableCalls)
    }

    @Test
    fun unavailableAuthenticationShowsDeviceLockGuidance() {
        composeRule.setContent {
            VelaTheme {
                AppLockSettingsScreen(
                    settings = AppLockSettings(),
                    isAuthenticationAvailable = false,
                    onBack = {},
                    onEnable = {},
                    onDisable = {},
                    onTimeoutChange = {},
                )
            }
        }

        composeRule.onNodeWithText("Hãy thiết lập khóa màn hình Android trước khi bật tính năng này.")
            .assertIsDisplayed()
    }

    @Test
    fun timeoutSheetDispatchesSelectedTimeout() {
        var selected: AppLockTimeout? = null
        composeRule.setContent {
            VelaTheme {
                AppLockSettingsScreen(
                    settings = AppLockSettings(enabled = true),
                    isAuthenticationAvailable = true,
                    onBack = {},
                    onEnable = {},
                    onDisable = {},
                    onTimeoutChange = { selected = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Thời gian khóa: 30 giây").performClick()
        composeRule.onNodeWithText("1 phút").performClick()

        assertEquals(AppLockTimeout.ONE_MINUTE, selected)
    }
}
