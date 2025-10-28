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

    // --- START OF FIX: Add isLoading state ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    // --- END OF FIX ---

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

    fun playSong(song: Song, strategy: AutoplayStrategy) {
        if (browser == null) return

        // --- START OF FIX: Set loading state before sending command ---
        _isLoading.value = true
        _nowPlaying.value = song // Optimistically set the song that is starting to play
        // --- END OF FIX ---

        val commandBundle = Bundle().apply {
            putParcelable("SONG_TO_PLAY", song)
            putParcelable("AUTOPLAY_STRATEGY", strategy)
        }
        browser?.sendCustomCommand(
            SessionCommand("PLAY_SONG", Bundle.EMPTY),
            commandBundle,
        )
    }

    override fun onCleared() {
        browser?.release()
        super.onCleared()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // When the media item changes in the service, update our state
            val song = mediaItem?.mediaMetadata?.extras?.getParcelable<Song>("SONG_METADATA")
            _nowPlaying.value = song
            // --- START OF FIX: When a new item starts playing, we are no longer loading ---
            _isLoading.value = false
            // --- END OF FIX ---
        }

        // --- START OF FIX: Also consider loading finished when playback starts ---
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _isLoading.value = false
            }
        }
        // --- END OF FIX ---
    }
}
