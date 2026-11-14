package cl.esucar.tutorvirtual.data.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import cl.esucar.tutorvirtual.domain.repository.RadioPlayerGateway
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Media3RadioGateway(context: Context) : RadioPlayerGateway {
    private val appContext = context.applicationContext

    private val _radioState = MutableStateFlow(RadioPlaybackState.STOPPED)
    override val radioState: StateFlow<RadioPlaybackState> = _radioState.asStateFlow()

    private val _radioError = MutableStateFlow<String?>(null)
    override val radioError: StateFlow<String?> = _radioError.asStateFlow()

    private val _radioVolume = MutableStateFlow(0.85f)
    override val radioVolume: StateFlow<Float> = _radioVolume.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingPlayOnConnect = false

    init {
        connectController()
    }

    @OptIn(UnstableApi::class)
    override fun play() {
        _radioError.value = null
        _radioState.value = RadioPlaybackState.BUFFERING
        val controller = mediaController
        if (controller != null) {
            pendingPlayOnConnect = false
            startStreamOnController(controller)
        } else {
            pendingPlayOnConnect = true
            connectController()
        }
    }

    override fun pause() {
        pendingPlayOnConnect = false
        mediaController?.let { controller ->
            controller.playWhenReady = false
            controller.pause()
        }
        _radioState.value = RadioPlaybackState.PAUSED
    }

    override fun resume() {
        val controller = mediaController
        if (controller == null ||
            controller.mediaItemCount == 0 ||
            controller.playbackState == Player.STATE_IDLE ||
            controller.playbackState == Player.STATE_ENDED
        ) {
            play()
            return
        }
        _radioError.value = null
        controller.volume = _radioVolume.value
        controller.playWhenReady = true
        controller.play()
        _radioState.value = if (controller.playbackState == Player.STATE_READY) {
            RadioPlaybackState.PLAYING
        } else {
            RadioPlaybackState.BUFFERING
        }
    }

    override fun stop() {
        pendingPlayOnConnect = false
        mediaController?.let { controller ->
            controller.playWhenReady = false
            controller.stop()
            controller.clearMediaItems()
            controller.sendCustomCommand(
                SessionCommand(RadioPlaybackService.ACTION_STOP_RADIO, Bundle.EMPTY),
                Bundle.EMPTY
            )
        }
        _radioError.value = null
        _radioState.value = RadioPlaybackState.STOPPED
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _radioVolume.value = clamped
        mediaController?.volume = clamped
    }

    override fun release() {
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun startStreamOnController(controller: MediaController) {
        controller.volume = _radioVolume.value
        controller.setMediaItem(RadioPlaybackService.buildRadioMediaItem(appContext))
        controller.prepare()
        controller.playWhenReady = true
        controller.play()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateFromController(mediaController)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateFromController(mediaController)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            syncStateFromController(mediaController)
        }

        override fun onVolumeChanged(volume: Float) {
            _radioVolume.value = volume.coerceIn(0f, 1f)
        }

        override fun onPlayerError(error: PlaybackException) {
            _radioState.value = RadioPlaybackState.ERROR
            _radioError.value = "No se pudo reproducir la señal en vivo. Verifica tu conexión a internet."
        }
    }

    private fun syncStateFromController(controller: MediaController?) {
        if (controller == null) return
        if (controller.playerError != null) {
            _radioState.value = RadioPlaybackState.ERROR
            _radioError.value = "No se pudo reproducir la señal en vivo. Verifica tu conexión a internet."
            return
        }
        when {
            controller.mediaItemCount == 0 || controller.playbackState == Player.STATE_IDLE -> {
                if (!pendingPlayOnConnect && _radioState.value != RadioPlaybackState.ERROR) {
                    _radioState.value = RadioPlaybackState.STOPPED
                }
            }
            controller.isPlaying -> {
                _radioError.value = null
                _radioState.value = RadioPlaybackState.PLAYING
            }
            controller.playbackState == Player.STATE_BUFFERING && controller.playWhenReady -> {
                _radioState.value = RadioPlaybackState.BUFFERING
            }
            controller.playbackState == Player.STATE_READY && !controller.playWhenReady -> {
                _radioState.value = RadioPlaybackState.PAUSED
            }
            controller.playbackState == Player.STATE_ENDED -> {
                _radioState.value = RadioPlaybackState.STOPPED
            }
        }
    }

    private fun connectController() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(appContext, ComponentName(appContext, RadioPlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (controller == null) {
                    controllerFuture = null
                    if (pendingPlayOnConnect) {
                        pendingPlayOnConnect = false
                        _radioState.value = RadioPlaybackState.ERROR
                        _radioError.value = "No se pudo conectar con el servicio de audio."
                    }
                    return@addListener
                }
                mediaController = controller
                controller.addListener(playerListener)
                if (pendingPlayOnConnect) {
                    pendingPlayOnConnect = false
                    startStreamOnController(controller)
                } else {
                    if (controller.mediaItemCount > 0) {
                        _radioVolume.value = controller.volume.coerceIn(0f, 1f)
                    } else {
                        controller.volume = _radioVolume.value
                    }
                    syncStateFromController(controller)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }
}
