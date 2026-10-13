package com.vela.notes.security

import com.vela.notes.data.preferences.AppLockSettings

fun shouldLockOnForeground(
    settings: AppLockSettings,
    backgroundedAt: Long?,
    now: Long,
): Boolean = settings.enabled && backgroundedAt != null && now - backgroundedAt >= settings.timeout.milliseconds
