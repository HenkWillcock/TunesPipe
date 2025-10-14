package com.example.tunespipe

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlayerService : MediaSessionService() {

    // This is the core object that connects our player to the rest of the Android system.
    private var mediaSession: MediaSession? = null

    // The system will call this to get the MediaSession object.
    // It's how the lock screen and other components will control our player.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // We will clean up the media session when the service is destroyed.
    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
