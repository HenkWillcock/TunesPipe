package com.example.tunespipe

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerViewModel : ViewModel() {

    private var browser: MediaBrowser? = null

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    fun initialize(context: Context) {
        if (browser == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
            val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
            browserFuture.addListener({
                browser = browserFuture.get()
                // Add listener for state changes
                browser?.addListener(PlayerListener())
            }, MoreExecutors.directExecutor())
        }
    }

    // --- START OF IMPLEMENTATION ---
    fun playSong(song: Song, strategy: AutoplayStrategy) {
        if (browser == null) return

        val commandBundle = Bundle().apply {
            putParcelable("SONG_TO_PLAY", song)
            putParcelable("AUTOPLAY_STRATEGY", strategy)
        }
        browser?.sendCustomCommand(
            SessionCommand("PLAY_SONG", Bundle.EMPTY),
            commandBundle,
        )
    }
    // --- END OF IMPLEMENTATION ---

    override fun onCleared() {
        browser?.release()
        super.onCleared()
    }

    // --- START OF IMPLEMENTATION ---
    private inner class PlayerListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // When the media item changes in the service, update our state
            val song = mediaItem?.mediaMetadata?.extras?.getParcelable<Song>("SONG_METADATA")
            _nowPlaying.value = song
        }
    }
    // --- END OF IMPLEMENTATION ---
}
