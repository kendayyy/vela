package com.vela.notes.data.scanner

enum class ScanPhase {
    SEARCHING,
    TRACKING,
    STABLE,
    CAPTURED,
}

data class ScanGuide(
    val phase: ScanPhase,
    val quad: DocumentQuad? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val countdown: Int? = null,
) {
    companion object {
        val Searching = ScanGuide(ScanPhase.SEARCHING)
    }
}

data class ScanFrameDecision(
    val guide: ScanGuide,
    val shouldCapture: Boolean,
)

class ScanCapturePlanner(
    private val stableFramesRequired: Int = 5,
    private val countdownStart: Int = 3,
) {
    private var reference: DocumentQuad? = null
    private var stableCount = 0
    private var countdown: Int? = null
    private var armed = true

    fun onFrame(quad: DocumentQuad?, imageWidth: Int, imageHeight: Int): ScanFrameDecision {
        val usable = quad?.takeIf { isValidDocumentQuad(it, imageWidth, imageHeight) }
        if (!armed) {
            if (usable == null) {
                armed = true
                resetTracking()
                return ScanFrameDecision(ScanGuide.Searching, false)
            }
            return ScanFrameDecision(
                ScanGuide(ScanPhase.CAPTURED, usable, imageWidth, imageHeight),
                shouldCapture = false,
            )
        }
        if (usable == null) {
            resetTracking()
            return ScanFrameDecision(ScanGuide.Searching, false)
        }
        val current = reference
        val drifted = current == null ||
            maxCornerDrift(current, usable) > stabilityTolerance(imageWidth, imageHeight)
        if (drifted) {
            reference = usable
            stableCount = 1
            countdown = null
            return ScanFrameDecision(
                ScanGuide(ScanPhase.TRACKING, usable, imageWidth, imageHeight),
                false,
            )
        }
        reference = usable
        stableCount += 1
        if (stableCount < stableFramesRequired) {
            countdown = null
            return ScanFrameDecision(
                ScanGuide(ScanPhase.TRACKING, usable, imageWidth, imageHeight),
                false,
            )
        }
        val shown = countdown ?: countdownStart
        countdown = shown - 1
        if (shown <= 0) {
            armed = false
            countdown = null
            return ScanFrameDecision(
                ScanGuide(ScanPhase.CAPTURED, usable, imageWidth, imageHeight),
                shouldCapture = true,
            )
        }
        return ScanFrameDecision(
            ScanGuide(ScanPhase.STABLE, usable, imageWidth, imageHeight, countdown = shown),
            false,
        )
    }

    fun captureManually(quad: DocumentQuad?, imageWidth: Int, imageHeight: Int): ScanFrameDecision {
        armed = false
        val usable = quad?.takeIf { isValidDocumentQuad(it, imageWidth, imageHeight) }
        return ScanFrameDecision(
            ScanGuide(ScanPhase.CAPTURED, usable, imageWidth, imageHeight),
            shouldCapture = true,
        )
    }

    private fun resetTracking() {
        reference = null
        stableCount = 0
        countdown = null
    }
}
