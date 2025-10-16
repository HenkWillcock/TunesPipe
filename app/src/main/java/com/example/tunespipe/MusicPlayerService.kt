package com.example.tunespipe

import android.app.Notification
import android.content.Intent
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: PlayerNotificationManager

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                mediaSession?.player?.seekTo(0)
                mediaSession?.player?.play()

                // TODO play next song in playlist or queue if there is one.
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayerSingleton.exoPlayer
            ?: throw IllegalStateException("ExoPlayer has not been initialized!")

        player.addListener(playerListener)

        mediaSession = MediaSession.Builder(this, player).build()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            1001, // Notification ID
            NOTIFICATION_CHANNEL_ID
        )
            // Use a NotificationListener to get access to the notification and manage the foreground state.
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
        mediaSession?.player?.removeListener(playerListener)
        mediaSession?.release()
        notificationManager.setPlayer(null)
        super.onDestroy()
    }
}
