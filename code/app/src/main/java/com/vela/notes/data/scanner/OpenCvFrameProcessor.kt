package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.os.SystemClock
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.max

internal const val MAX_ANALYSIS_EDGE_PX = 480

data class FrameProcessingResult(
    val width: Int,
    val height: Int,
    val elapsedMs: Long,
)

class OpenCvFrameProcessor {
    fun ensureLoaded() {
        OpenCvRuntime.ensureLoaded()
    }

    fun process(bitmap: Bitmap): FrameProcessingResult {
        ensureLoaded()
        val scaled = scaleDown(bitmap)
        val source = Mat()
        val gray = Mat()
        val started = SystemClock.elapsedRealtime()
        try {
            Utils.bitmapToMat(scaled, source)
            Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)
            return FrameProcessingResult(
                width = gray.cols(),
                height = gray.rows(),
                elapsedMs = SystemClock.elapsedRealtime() - started,
            )
        } finally {
            source.release()
            gray.release()
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longestEdge = max(bitmap.width, bitmap.height)
        if (longestEdge <= MAX_ANALYSIS_EDGE_PX) return bitmap
        val scale = MAX_ANALYSIS_EDGE_PX.toFloat() / longestEdge
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return scaled
    }
}
