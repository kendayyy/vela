package com.vela.notes.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

interface AudioRecorderEngine {
    fun start(onMaximumDurationReached: () -> Unit): File
    fun pause()
    fun resume()
    fun stop(): File
    fun cancel()
    fun cleanupStaleFiles(): Int
}

class AndroidAudioRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorderEngine {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun start(onMaximumDurationReached: () -> Unit): File {
        check(recorder == null) { "Recorder is already active" }
        val directory = recordingDirectory(context).apply { mkdirs() }
        val output = File(directory, "${UUID.randomUUID()}.m4a.part")
        val mediaRecorder = createRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(AUDIO_BIT_RATE)
            setAudioSamplingRate(AUDIO_SAMPLE_RATE)
            setMaxDuration(MAX_DURATION_MILLIS)
            setOutputFile(output.absolutePath)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    onMaximumDurationReached()
                }
            }
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = output
        return output
    }

    override fun pause() {
        checkNotNull(recorder) { "Recorder is not active" }.pause()
    }

    override fun resume() {
        checkNotNull(recorder) { "Recorder is not active" }.resume()
    }

    override fun stop(): File {
        val active = checkNotNull(recorder) { "Recorder is not active" }
        val partial = checkNotNull(outputFile)
        return try {
            active.stop()
            val completed = File(partial.parentFile, partial.name.removeSuffix(".part"))
            if (!partial.renameTo(completed)) throw IOException("Unable to finalize audio recording")
            completed
        } catch (error: RuntimeException) {
            partial.delete()
            throw IOException("Audio recording was too short or invalid", error)
        } finally {
            active.reset()
            active.release()
            recorder = null
            outputFile = null
        }
    }

    override fun cancel() {
        val active = recorder
        recorder = null
        runCatching { active?.stop() }
        runCatching { active?.reset() }
        active?.release()
        outputFile?.delete()
        outputFile = null
    }

    override fun cleanupStaleFiles(): Int {
        val directory = recordingDirectory(context)
        if (!directory.exists()) return 0
        var deleted = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && (file.name.endsWith(".part") || file.name.endsWith(".m4a")) && file.delete()) {
                deleted += 1
            }
        }
        return deleted
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(context: Context): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    companion object {
        const val MAX_DURATION_MILLIS = 60 * 60 * 1_000
        private const val AUDIO_BIT_RATE = 128_000
        private const val AUDIO_SAMPLE_RATE = 44_100

        fun recordingDirectory(context: Context) = File(context.filesDir, ".audio-recording")
    }
}
