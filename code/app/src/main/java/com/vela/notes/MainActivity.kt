package com.vela.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vela.notes.security.AppLockViewModel
import com.vela.notes.ui.VelaApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appLockViewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                appLockViewModel.uiState
                    .map { it.settings.enabled }
                    .distinctUntilChanged()
                    .collect { enabled ->
                        if (enabled) {
                            window.setFlags(
                                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
            }
        }
        setContent {
            VelaApp(appLockViewModel)
        }
    }
}
