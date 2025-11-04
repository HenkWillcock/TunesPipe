package com.example.tunespipe

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {

    private val _browser = MutableStateFlow<MediaBrowser?>(null)
    val browserFlow = _browser.asStateFlow()
    private var browser: MediaBrowser?
        get() = _browser.value
        set(value) {
            _browser.value = value
        }

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun initialize(context: Context) {
        if (browser == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
            viewModelScope.launch {
                val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
                browser = browserFuture.await()
                browser?.addListener(PlayerStateListener())
                _nowPlaying.value = browser?.currentMediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
            }
        }
    }

    fun playSongFromPlaylist(songs: List<Song>, startIndex: Int, shuffle: Boolean) {
        if (browser == null) return

        _isLoading.value = true
        _nowPlaying.value = songs[startIndex]

        val commandBundle = Bundle().apply {
            putParcelableArrayList("SONGS", ArrayList(songs))
            putInt("START_INDEX", startIndex)
            putBoolean("SHUFFLE", shuffle)
        }
        browser?.sendCustomCommand(
            SessionCommand("SET_PLAYLIST", Bundle.EMPTY),
            commandBundle,
        )
    }

    // --- START OF NEW QUEUE-HANDLING LOGIC ---
    fun addToQueue(song: Song) {
        if (browser == null) return
        browser?.addMediaItem(song.toMediaItem())
    }

    fun playNext(song: Song) {
        if (browser == null) return
        val currentIndex = browser?.currentMediaItemIndex ?: -1
        if (currentIndex != -1) {
            browser?.addMediaItem(currentIndex + 1, song.toMediaItem())
        } else {
            // If nothing is playing, just add it to the end of the (potentially empty) queue.
            browser?.addMediaItem(song.toMediaItem())
        }
    }

    // Helper function to convert a Song to a placeholder MediaItem
    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(this.trackName)
            .setArtist(this.artistName)
            .setArtworkUri(android.net.Uri.parse(this.artworkUrl))
            .setExtras(Bundle().apply { putParcelable("SONG_METADATA", this@toMediaItem) })
            .build()

        return MediaItem.Builder()
            .setMediaId(this.trackId)
            .setMediaMetadata(metadata)
            .build()
    }
    // --- END OF NEW QUEUE-HANDLING LOGIC ---

    override fun onCleared() {
        browser?.release()
        super.onCleared()
    }

    private inner class PlayerStateListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _isLoading.value = true // A transition starts a new loading process
            _nowPlaying.value = mediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Once playback actually starts, we are no longer loading.
            if (isPlaying) {
                _isLoading.value = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            // It's also a good idea to move to the next item on error
            browser?.seekToNext()
        }
    }
}
