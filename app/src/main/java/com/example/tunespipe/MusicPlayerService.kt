package com.example.tunespipe

import android.app.Notification
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayerSingleton.exoPlayer
            ?: throw IllegalStateException("ExoPlayer has not been initialized!")

        mediaSession = MediaSession.Builder(this, player).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            1001, // Notification ID
            NOTIFICATION_CHANNEL_ID
        )
            // --- THIS IS THE KEY ---
            // Use a NotificationListener to get access to the notification and manage the foreground state.
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    // When the notification is posted, promote the service to the foreground.
                    // The 'ongoing' flag is true when the media is playing.
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        // If the 'ongoing' flag is false (e.g., player is paused), demote the service.
                        // This makes the notification dismissible.
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    // When the notification is cancelled (e.g., swiped away), stop the service.
                    stopSelf()
                }
            })
            // --- END OF KEY CHANGE ---
            .build().apply {
                setPlayer(player)
                setMediaSessionToken(mediaSession!!.platformToken)
            }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop the service if the task is removed and the player is not playing.
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        notificationManager.setPlayer(null)
        super.onDestroy()
    }
}
