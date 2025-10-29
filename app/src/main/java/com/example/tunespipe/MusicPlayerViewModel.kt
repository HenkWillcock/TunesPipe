package com.example.tunespipe

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {

    private var browser: MediaBrowser? = null

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _strategy = MutableStateFlow<AutoplayStrategy?>(null)
    val strategy = _strategy.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()

    fun initialize(context: Context) {
        if (browser == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
            viewModelScope.launch {
                val browserFuture = MediaBrowser.Builder(context, sessionToken)
                    .setListener(BrowserListener())
                    .buildAsync()
                browser = browserFuture.await()
                browser?.addListener(PlayerStateListener())
                _nowPlaying.value = browser?.currentMediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
            }
        }
    }

    fun playSong(song: Song, strategy: AutoplayStrategy) {
        if (browser == null) return

        _isLoading.value = true
        _nowPlaying.value = song
        _strategy.value = strategy

        val commandBundle = Bundle().apply {
            putParcelable("SONG_TO_PLAY", song)
            putParcelable("AUTOPLAY_STRATEGY", strategy)
            // When we start a new song, we clear the ViewModel's queue and send that empty queue
            // This ensures we don't have stale songs from a previous session.
            _queue.value = emptyList()
            putParcelableArrayList("QUEUE_SONGS", ArrayList())
        }
        browser?.sendCustomCommand(
            SessionCommand("PLAY_SONG", Bundle.EMPTY),
            commandBundle,
        )
    }

    fun playNext(song: Song) {
        _queue.value = listOf(song) + _queue.value
        sendQueueUpdateToService()
    }

    fun addSongToQueue(song: Song) {
        _queue.value = _queue.value + song
        sendQueueUpdateToService()
    }

    private fun sendQueueUpdateToService() {
        if (browser == null) return

        val queueToSend = ArrayList(_queue.value)

        val commandBundle = Bundle().apply {
            putParcelableArrayList("QUEUE_SONGS", queueToSend)
        }
        browser?.sendCustomCommand(
            SessionCommand("UPDATE_QUEUE", Bundle.EMPTY),
            commandBundle
        )
    }

    override fun onCleared() {
        browser?.release()
        super.onCleared()
    }

    // --- START OF FIX: Simplified BrowserListener ---
    private inner class BrowserListener : MediaBrowser.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            // This listener now ONLY handles queue updates from the service
            if (command.customAction == "UPDATE_QUEUE") {
                val queueSongs = args.getParcelableArrayList<Song>("QUEUE_SONGS")
                if (queueSongs != null) {
                    _queue.value = queueSongs
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
    // --- END OF FIX ---

    private inner class PlayerStateListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _isLoading.value = false
            _nowPlaying.value = mediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _isLoading.value = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
        }
    }
}
