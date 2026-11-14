package cl.esucar.tutorvirtual.domain.repository

import cl.esucar.tutorvirtual.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface RadioPlayerGateway {
    val radioState: StateFlow<RadioPlaybackState>
    val radioError: StateFlow<String?>
    val radioVolume: StateFlow<Float>

    fun play()
    fun pause()
    fun resume()
    fun stop()
    fun setVolume(volume: Float)
    fun release()
}
