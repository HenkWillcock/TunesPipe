package com.example.tunespipe

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayerSingleton.exoPlayer
            ?: throw IllegalStateException("ExoPlayer has not been initialized!")

        mediaSession = MediaSession.Builder(this, player).build()

        // The PlayerNotificationManager is smart enough to handle the foreground service
        // and notification display on its own when a MediaSession is provided.
        notificationManager = PlayerNotificationManager.Builder(
            this,
            1001,
            NOTIFICATION_CHANNEL_ID
        ).build().apply {
            setPlayer(player)
            setMediaSessionToken(mediaSession!!.platformToken)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            // If the player is paused or has nothing to play, stop the service.
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        notificationManager?.setPlayer(null)
        super.onDestroy()
    }
}
