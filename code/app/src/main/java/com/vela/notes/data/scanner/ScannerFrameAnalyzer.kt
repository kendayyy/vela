package com.vela.notes.data.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ScannerFrameAnalyzer(
    private val gate: FrameAnalysisGate = FrameAnalysisGate(),
    private val onFrame: (ImageProxy) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        if (!gate.tryAcquire()) {
            image.close()
            return
        }
        try {
            onFrame(image)
        } finally {
            image.close()
            gate.release()
        }
    }
}
