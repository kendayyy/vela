package com.vela.notes.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vela.notes.data.preferences.AppLockSettings
import com.vela.notes.data.preferences.AppLockSettingsStore
import com.vela.notes.data.preferences.AppLockTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun interface ElapsedRealtimeClock {
    fun now(): Long
}

enum class AppLockAuthenticationPurpose {
    UNLOCK,
    DISABLE,
}

data class AppLockAuthenticationRequest(
    val id: Long,
    val purpose: AppLockAuthenticationPurpose,
)

data class AppLockUiState(
    val ready: Boolean = false,
    val settings: AppLockSettings = AppLockSettings(),
    val isLocked: Boolean = true,
    val authenticationRequest: AppLockAuthenticationRequest? = null,
)

@Singleton
class AppLockCoordinator @Inject constructor(
    private val settingsStore: AppLockSettingsStore,
    private val clock: ElapsedRealtimeClock,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(AppLockUiState())
    private var wasEnabled = false
    private var backgroundedAt: Long? = null
    private var requestId = 0L

    val state: StateFlow<AppLockUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            settingsStore.settings.collect(::applySettings)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        onAppBackgrounded()
    }

    fun onAppBackgrounded() {
        if (mutableState.value.settings.enabled) backgroundedAt = clock.now()
    }

    fun onAppForegrounded() {
        val state = mutableState.value
        val startedAt = backgroundedAt ?: return
        backgroundedAt = null
        if (shouldLockOnForeground(state.settings, startedAt, clock.now())) {
            mutableState.value = state.copy(isLocked = true, authenticationRequest = null)
        }
    }

    fun requestUnlock() {
        if (!mutableState.value.isLocked) return
        requestAuthentication(AppLockAuthenticationPurpose.UNLOCK)
    }

    fun requestDisable() {
        if (!mutableState.value.settings.enabled) return
        requestAuthentication(AppLockAuthenticationPurpose.DISABLE)
    }

    fun onAuthenticationResult(authenticated: Boolean) {
        val request = mutableState.value.authenticationRequest ?: return
        mutableState.value = mutableState.value.copy(authenticationRequest = null)
        if (!authenticated) return

        when (request.purpose) {
            AppLockAuthenticationPurpose.UNLOCK -> {
                mutableState.value = mutableState.value.copy(isLocked = false)
            }
            AppLockAuthenticationPurpose.DISABLE -> {
                scope.launch { settingsStore.setEnabled(false) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch { settingsStore.setEnabled(enabled) }
    }

    fun setTimeout(timeout: AppLockTimeout) {
        scope.launch { settingsStore.setTimeout(timeout) }
    }

    private fun requestAuthentication(purpose: AppLockAuthenticationPurpose) {
        requestId += 1
        mutableState.value = mutableState.value.copy(
            authenticationRequest = AppLockAuthenticationRequest(requestId, purpose),
        )
    }

    private fun applySettings(settings: AppLockSettings) {
        val previous = mutableState.value
        val justEnabled = settings.enabled && !wasEnabled
        wasEnabled = settings.enabled
        mutableState.value = previous.copy(
            ready = true,
            settings = settings,
            isLocked = when {
                !settings.enabled -> false
                !previous.ready -> true
                justEnabled -> true
                else -> previous.isLocked
            },
            authenticationRequest = if (settings.enabled) previous.authenticationRequest else null,
        )
    }
}
