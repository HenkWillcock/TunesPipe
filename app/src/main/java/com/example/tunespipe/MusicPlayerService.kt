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

    // The old, buggy playerListener has been completely removed.

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayerSingleton.exoPlayer
            ?: throw IllegalStateException("ExoPlayer has not been initialized!")

        // Do NOT add any listeners to the player here.
        // This ensures the Singleton has exclusive control.
        mediaSession = MediaSession.Builder(this, player).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            1001, // Notification ID
            NOTIFICATION_CHANNEL_ID
        )
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }
            })
            .build().apply {
                setPlayer(player)
                setMediaSessionToken(mediaSession!!.platformToken)
            }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // There is no listener to remove anymore.
        mediaSession?.release()
        notificationManager.setPlayer(null)
        super.onDestroy()
    }
}
