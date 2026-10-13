package com.vela.notes.data.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformPeakCollectorTest {
    @Test
    fun emptyInputProducesStableSilentWaveform() {
        assertArrayEquals(ByteArray(4), WaveformPeakCollector().toByteArray(4))
    }

    @Test
    fun peaksAreCompressedAndNormalized() {
        val collector = WaveformPeakCollector(samplesPerChunk = 2)
        listOf<Short>(1, -2, 4, -8, 10, -20, 30, -40).forEach(collector::add)

        assertArrayEquals(byteArrayOf(5, 20, 50, 100), collector.toByteArray(4))
    }

    @Test
    fun minimumShortDoesNotOverflow() {
        val collector = WaveformPeakCollector(samplesPerChunk = 1)
        collector.add(Short.MIN_VALUE)

        assertEquals(100, collector.toByteArray(1).single().toInt())
    }
}
