package com.vela.notes.data.scanner

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerAnalysisPerformanceTest {
    @Test
    fun openCvAnalysisStaysWithinBudgetAfterDownscale() {
        val processor = OpenCvFrameProcessor()
        processor.ensureLoaded()
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF226688.toInt())
        try {
            val first = processor.process(bitmap)
            val samples = List(20) { processor.process(bitmap) }
            val slowest = samples.maxOf { it.elapsedMs }
            assertTrue(max(first.width, first.height) <= MAX_ANALYSIS_EDGE_PX)
            assertEquals(480, first.width)
            assertEquals(360, first.height)
            assertTrue(
                "Slowest analysis frame took ${slowest}ms",
                slowest <= FRAME_BUDGET_MS,
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun analyzerClosesDroppedFramesWhileOneIsInFlight() {
        val gate = FrameAnalysisGate()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val analyzed = AtomicInteger(0)
        val analyzer = ScannerFrameAnalyzer(gate) {
            analyzed.incrementAndGet()
            started.countDown()
            release.await()
        }
        val closed = AtomicInteger(0)
        val worker = thread {
            analyzer.analyze(FakeImageProxy(closed))
        }
        assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS))

        repeat(10) { analyzer.analyze(FakeImageProxy(closed)) }

        assertEquals(1, analyzed.get())
        assertEquals(10, closed.get())
        assertEquals(10, gate.dropped)

        release.countDown()
        worker.join(2_000)
        assertEquals(11, closed.get())
        assertEquals(false, worker.isAlive)
    }

    private class FakeImageProxy(
        private val closed: AtomicInteger,
    ) : ImageProxy {
        override fun close() {
            closed.incrementAndGet()
        }

        override fun getCropRect(): Rect = Rect()

        override fun setCropRect(cropRect: Rect?) = Unit

        override fun getFormat(): Int = 1

        override fun getHeight(): Int = 480

        override fun getWidth(): Int = 640

        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = emptyArray()

        override fun getImageInfo(): ImageInfo = throw UnsupportedOperationException()

        override fun getImage(): Image? = null
    }

    private companion object {
        const val FRAME_BUDGET_MS = 200L
    }
}
