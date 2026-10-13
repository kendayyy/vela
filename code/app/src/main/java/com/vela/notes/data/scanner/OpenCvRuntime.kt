package com.vela.notes.data.scanner

import org.opencv.android.OpenCVLoader

internal object OpenCvRuntime {
    @Synchronized
    fun ensureLoaded() {
        check(OpenCVLoader.initLocal()) { "OpenCV failed to load" }
    }
}
