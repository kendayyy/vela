package com.vela.notes.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vela.notes.security.AppLockAuthenticationPurpose
import com.vela.notes.security.AppLockAuthenticationRequest

private const val APP_LOCK_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

fun canAuthenticateForAppLock(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(APP_LOCK_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

@Composable
fun AppLockAuthenticationEffect(
    request: AppLockAuthenticationRequest?,
    onAuthenticationResult: (Boolean) -> Unit,
) {
    val activity = LocalActivity.current as? FragmentActivity ?: return
    LaunchedEffect(request?.id) {
        val activeRequest = request ?: return@LaunchedEffect
        val title = when (activeRequest.purpose) {
            AppLockAuthenticationPurpose.UNLOCK -> "Mở khóa Vela"
            AppLockAuthenticationPurpose.DISABLE -> "Tắt khóa ứng dụng"
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthenticationResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onAuthenticationResult(false)
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Dùng sinh trắc học hoặc khóa màn hình Android")
                .setAllowedAuthenticators(APP_LOCK_AUTHENTICATORS)
                .build(),
        )
    }
}
