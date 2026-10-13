package com.vela.notes.data.scanner

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FrameAnalysisGate {
    private val active = AtomicBoolean(false)
    private val acceptedCount = AtomicInteger(0)
    private val droppedCount = AtomicInteger(0)

    val accepted: Int get() = acceptedCount.get()
    val dropped: Int get() = droppedCount.get()

    fun tryAcquire(): Boolean {
        val acquired = active.compareAndSet(false, true)
        if (acquired) acceptedCount.incrementAndGet() else droppedCount.incrementAndGet()
        return acquired
    }

    fun release() {
        active.set(false)
    }
}
