package com.vela.notes.security

import androidx.lifecycle.ViewModel
import com.vela.notes.data.preferences.AppLockTimeout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val coordinator: AppLockCoordinator,
) : ViewModel() {
    val uiState: StateFlow<AppLockUiState> = coordinator.state

    fun unlock() = coordinator.requestUnlock()

    fun disable() = coordinator.requestDisable()

    fun onAuthenticationResult(authenticated: Boolean) = coordinator.onAuthenticationResult(authenticated)

    fun setEnabled(enabled: Boolean) = coordinator.setEnabled(enabled)

    fun setTimeout(timeout: AppLockTimeout) = coordinator.setTimeout(timeout)
}
