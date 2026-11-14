package cl.esucar.tutorvirtual.radio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import cl.esucar.tutorvirtual.MainActivity
import cl.esucar.tutorvirtual.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream

class RadioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createRadioNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(RADIO_CHANNEL_ID)
            .setChannelName(R.string.radio_channel_name)
            .setNotificationId(RADIO_NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_radio_notification)
            }
        setMediaNotificationProvider(notificationProvider)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent("TutorVirtualESUCAR/1.0 (Android)")

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(RadioSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createRadioNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            RADIO_CHANNEL_ID,
            getString(R.string.radio_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de reproducción en vivo de Radio Carabineros de Chile"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    @OptIn(UnstableApi::class)
    private inner class RadioSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_STOP_RADIO, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_STOP_RADIO) {
                session.player.playWhenReady = false
                session.player.stop()
                session.player.clearMediaItems()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    listOf(buildRadioMediaItem(this@RadioPlaybackService)),
                    0,
                    C.TIME_UNSET
                )
            )
        }
    }

    companion object {
        const val RADIO_STREAM_URL = "https://stream.zeno.fm/533gym64ckhvv"
        const val RADIO_CHANNEL_ID = "radio_playback"
        const val RADIO_NOTIFICATION_ID = 3001
        const val ACTION_STOP_RADIO = "cl.esucar.tutorvirtual.ACTION_STOP_RADIO"

        @Volatile
        private var cachedArtworkBytes: ByteArray? = null

        fun buildRadioMediaItem(context: Context): MediaItem {
            val artwork = getOrLoadArtworkBytes(context)
            val metadataBuilder = MediaMetadata.Builder()
                .setTitle("Radio Carabineros de Chile")
                .setArtist("En vivo · FM 98.1 / AM 820")
                .setAlbumTitle("Tutor Virtual ESUCAR")
                .setStation("Radio Carabineros de Chile")
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            if (artwork != null) {
                metadataBuilder.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }

            return MediaItem.Builder()
                .setMediaId("radio-carabineros-live")
                .setUri(RADIO_STREAM_URL)
                .setMediaMetadata(metadataBuilder.build())
                .setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
                .build()
        }

        private fun getOrLoadArtworkBytes(context: Context): ByteArray? {
            cachedArtworkBytes?.let { return it }
            return runCatching {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.institutional_header_logo)
                    ?: return null
                ByteArrayOutputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
                    stream.toByteArray().also { cachedArtworkBytes = it }
                }
            }.getOrNull()
        }
    }
}
