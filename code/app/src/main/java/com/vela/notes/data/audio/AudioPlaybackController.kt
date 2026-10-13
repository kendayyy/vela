package com.vela.notes.data.audio

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vela.notes.data.attachment.AttachmentStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val MIN_PLAYBACK_SPEED = 0.5f
internal const val MAX_PLAYBACK_SPEED = 2f
internal const val DEFAULT_PLAYBACK_SPEED = 1f
internal val PLAYBACK_SPEED_OPTIONS = listOf(0.5f, 1f, 1.5f, 2f)

internal fun clampPlaybackPosition(positionMs: Long, durationMs: Long): Long =
    positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))

internal fun clampPlaybackSpeed(speed: Float): Float {
    if (!speed.isFinite()) return DEFAULT_PLAYBACK_SPEED
    return speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
}

internal fun playbackStateForAttachment(attachmentId: String, retainedSpeed: Float): AudioPlaybackState =
    AudioPlaybackState(
        attachmentId = attachmentId,
        speed = clampPlaybackSpeed(retainedSpeed),
    )

internal fun AudioPlaybackState.released(): AudioPlaybackState = AudioPlaybackState()

data class AudioPlaybackState(
    val attachmentId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = DEFAULT_PLAYBACK_SPEED,
    val errorMessage: String? = null,
)

interface AudioPlaybackController {
    val state: StateFlow<AudioPlaybackState>
    fun toggle(attachmentId: String, relativePath: String)
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun release()
}

class AndroidAudioPlaybackController internal constructor(
    context: Context,
    private val uriResolver: AudioUriResolver,
) : AudioPlaybackController {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        attachmentStore: AttachmentStore,
    ) : this(context, AudioUriResolver { path -> Uri.fromFile(attachmentStore.resolve(path)) })

    private val player = createPlayer(context)
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(AudioPlaybackState())
    override val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()
    private var released = false

    private val progressUpdate = object : Runnable {
        override fun run() {
            if (released) return
            updateProgress()
            if (player.isPlaying) handler.postDelayed(this, PROGRESS_INTERVAL_MILLIS)
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (released) return
                _state.value = _state.value.copy(isPlaying = isPlaying)
                handler.removeCallbacks(progressUpdate)
                if (isPlaying) handler.post(progressUpdate) else updateProgress()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (released) return
                updateProgress()
                if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                    player.pause()
                    updateProgress()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (released) return
                updateProgress()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (released) return
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = "Không thể phát bản ghi âm.",
                )
            }
        })
    }

    override fun toggle(attachmentId: String, relativePath: String) {
        requireMainThread()
        if (released) return
        if (_state.value.attachmentId != attachmentId) {
            player.setMediaItem(MediaItem.fromUri(uriResolver.resolve(relativePath)))
            player.prepare()
            _state.value = playbackStateForAttachment(attachmentId, _state.value.speed)
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun seekTo(positionMs: Long) {
        requireMainThread()
        if (released || _state.value.attachmentId == null) return
        updateProgress()
        player.seekTo(clampPlaybackPosition(positionMs, _state.value.durationMs))
        updateProgress()
    }

    override fun setSpeed(speed: Float) {
        requireMainThread()
        if (released) return
        val safeSpeed = clampPlaybackSpeed(speed)
        player.playbackParameters = PlaybackParameters(safeSpeed)
        _state.value = _state.value.copy(speed = player.playbackParameters.speed)
    }

    override fun release() {
        requireMainThread()
        if (released) return
        released = true
        handler.removeCallbacks(progressUpdate)
        player.release()
        _state.value = _state.value.released()
    }

    private fun updateProgress() {
        if (released) return
        val duration = player.duration.takeUnless { it == C.TIME_UNSET || it < 0 } ?: 0
        _state.value = _state.value.copy(
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = duration,
        )
    }

    private fun createPlayer(context: Context): ExoPlayer {
        requireMainThread()
        return ExoPlayer.Builder(context).build()
    }

    private fun requireMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Audio playback must run on the main thread."
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MILLIS = 250L
    }
}

internal fun interface AudioUriResolver {
    fun resolve(relativePath: String): Uri
}
