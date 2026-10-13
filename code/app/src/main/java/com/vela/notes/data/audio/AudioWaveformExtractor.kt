package com.vela.notes.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AudioWaveformExtractor {
    suspend fun extract(file: File, peakCount: Int = DEFAULT_WAVEFORM_PEAKS): ByteArray
}

class AndroidAudioWaveformExtractor @Inject constructor() : AudioWaveformExtractor {
    override suspend fun extract(file: File, peakCount: Int): ByteArray = withContext(Dispatchers.Default) {
        require(peakCount > 0) { "Peak count must be positive" }
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("Audio track not found")
            val format = extractor.getTrackFormat(trackIndex)
            val mimeType = checkNotNull(format.getString(MediaFormat.KEY_MIME))
            extractor.selectTrack(trackIndex)
            val codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(format, null, null, 0)
                start()
            }
            decoder = codec

            val collector = WaveformPeakCollector()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = checkNotNull(codec.getInputBuffer(inputIndex))
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
                if (outputIndex >= 0) {
                    codec.getOutputBuffer(outputIndex)?.let { buffer ->
                        val pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).apply {
                            position(info.offset)
                            limit(info.offset + info.size)
                        }
                        while (pcm.remaining() >= Short.SIZE_BYTES) collector.add(pcm.short)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                }
            }
            collector.toByteArray(peakCount)
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }
}

internal class WaveformPeakCollector(
    private val samplesPerChunk: Int = 1_024,
) {
    private val chunkPeaks = mutableListOf<Int>()
    private var currentPeak = 0
    private var currentSampleCount = 0

    init {
        require(samplesPerChunk > 0)
    }

    fun add(sample: Short) {
        val magnitude = if (sample == Short.MIN_VALUE) Short.MAX_VALUE.toInt() else kotlin.math.abs(sample.toInt())
        currentPeak = maxOf(currentPeak, magnitude)
        currentSampleCount += 1
        if (currentSampleCount == samplesPerChunk) flushChunk()
    }

    fun toByteArray(peakCount: Int): ByteArray {
        require(peakCount > 0)
        if (currentSampleCount > 0) flushChunk()
        if (chunkPeaks.isEmpty()) return ByteArray(peakCount)
        val maximum = chunkPeaks.max().coerceAtLeast(1)
        return ByteArray(peakCount) { outputIndex ->
            val start = outputIndex * chunkPeaks.size / peakCount
            val end = maxOf(start + 1, (outputIndex + 1) * chunkPeaks.size / peakCount)
                .coerceAtMost(chunkPeaks.size)
            val peak = if (start < chunkPeaks.size) {
                chunkPeaks.subList(start, end).max()
            } else {
                chunkPeaks.last()
            }
            ((peak.toLong() * WAVEFORM_MAX_VALUE) / maximum).toInt().toByte()
        }
    }

    private fun flushChunk() {
        chunkPeaks += currentPeak
        currentPeak = 0
        currentSampleCount = 0
    }
}

const val DEFAULT_WAVEFORM_PEAKS = 64
private const val WAVEFORM_MAX_VALUE = 100
private const val CODEC_TIMEOUT_US = 10_000L
