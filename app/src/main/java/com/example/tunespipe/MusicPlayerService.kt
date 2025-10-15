package com.example.tunespipe

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    // This method is called when the service is first created.
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(
            this,
            MusicPlayerSingleton.exoPlayer!!,
        ).build()

        // Create the notification manager
        notificationManager = PlayerNotificationManager.Builder(
            this,
            1001, // A unique ID for the notification
            "tunespipe_media_playback"
        ).build()

        // Link the notification manager to the player and the media session
        notificationManager?.setPlayer(MusicPlayerSingleton.exoPlayer!!)
        notificationManager?.setMediaSessionToken(mediaSession!!.platformToken)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.release()
        MusicPlayerSingleton.exoPlayer?.release()
        super.onDestroy()
    }
}
