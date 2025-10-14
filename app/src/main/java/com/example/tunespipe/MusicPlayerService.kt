package com.example.tunespipe

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // This method is called when the service is first created.
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession.Builder(
            this,
            MusicPlayer.getOrCreatePlayer(this),
        ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
