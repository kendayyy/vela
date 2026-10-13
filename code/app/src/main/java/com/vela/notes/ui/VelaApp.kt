package com.vela.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vela.notes.navigation.VelaNavHost
import com.vela.notes.security.AppLockViewModel
import com.vela.notes.ui.security.AppLockAuthenticationEffect
import com.vela.notes.ui.security.AppLockScreen
import com.vela.notes.ui.shell.FocusCapsuleState
import com.vela.notes.ui.shell.VelaAppFrame
import com.vela.notes.ui.theme.VelaTheme

@Composable
fun VelaApp(appLockViewModel: AppLockViewModel) {
    val appLockState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    VelaTheme {
        AppLockAuthenticationEffect(
            request = appLockState.authenticationRequest,
            onAuthenticationResult = appLockViewModel::onAuthenticationResult,
        )
        if (appLockState.ready && appLockState.isLocked) {
            AppLockScreen(onUnlock = appLockViewModel::unlock)
        } else if (appLockState.ready) {
            var focusState by remember { mutableStateOf<FocusCapsuleState>(FocusCapsuleState.Hidden) }
            var onCapsuleClick by remember { mutableStateOf<(() -> Unit)?>(null) }
            VelaAppFrame(focusState = focusState, onCapsuleClick = onCapsuleClick) {
                VelaNavHost(
                    appLockState = appLockState,
                    onAppLockEnabledChange = appLockViewModel::setEnabled,
                    onAppLockDisable = appLockViewModel::disable,
                    onAppLockTimeoutChange = appLockViewModel::setTimeout,
                    onFocusStateChange = { focusState = it },
                    onCapsuleClickChange = { onCapsuleClick = it },
                )
            }
        }
    }
}
