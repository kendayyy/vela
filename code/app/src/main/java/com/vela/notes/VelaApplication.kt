package com.vela.notes

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.vela.notes.data.audio.AudioRecorderEngine
import com.vela.notes.security.AppLockCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VelaApplication : Application() {
    @Inject lateinit var audioRecorderEngine: AudioRecorderEngine
    @Inject lateinit var appLockCoordinator: AppLockCoordinator

    override fun onCreate() {
        super.onCreate()
        audioRecorderEngine.cleanupStaleFiles()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockCoordinator)
    }
}
