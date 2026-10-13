package com.vela.notes.security

import com.vela.notes.data.preferences.AppLockSettings
import com.vela.notes.data.preferences.AppLockTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {
    @Test
    fun doesNotLockWhenFeatureIsDisabled() {
        assertFalse(
            shouldLockOnForeground(
                settings = AppLockSettings(enabled = false),
                backgroundedAt = 1_000L,
                now = 100_000L,
            ),
        )
    }

    @Test
    fun locksImmediatelyWhenImmediateTimeoutIsSelected() {
        assertTrue(
            shouldLockOnForeground(
                settings = AppLockSettings(enabled = true, timeout = AppLockTimeout.IMMEDIATELY),
                backgroundedAt = 10_000L,
                now = 10_000L,
            ),
        )
    }

    @Test
    fun respectsThirtySecondTimeoutBoundary() {
        val settings = AppLockSettings(enabled = true, timeout = AppLockTimeout.THIRTY_SECONDS)

        assertFalse(shouldLockOnForeground(settings, backgroundedAt = 10_000L, now = 39_999L))
        assertTrue(shouldLockOnForeground(settings, backgroundedAt = 10_000L, now = 40_000L))
    }
}
