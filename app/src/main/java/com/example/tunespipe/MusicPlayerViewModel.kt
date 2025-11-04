package com.example.tunespipe

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {

    private var browser: MediaBrowser? = null

    private val _playerPosition = MutableLiveData<Long>()
    val playerPosition: LiveData<Long> = _playerPosition
    private var progressJob: Job? = null

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _manualQueueCount = MutableStateFlow(0)
    val manualQueueCount = _manualQueueCount.asStateFlow()

    private val _playerState = MutableStateFlow<Player?>(null)
    val playerState: LiveData<Player?> = _playerState.asLiveData()

    fun playSong(songs: List<Song>, startIndex: Int, shuffle: Boolean, repeat: Boolean) {
        if (browser == null || songs.isEmpty()) return

        _isLoading.value = true
        _nowPlaying.value = songs[startIndex]

        val commandBundle = Bundle().apply {
            putParcelableArrayList("SONGS_TO_PLAY", ArrayList(songs))
            putInt("START_INDEX", startIndex)
            // --- Add these new parameters ---
            putBoolean("SHUFFLE", shuffle)
            putBoolean("REPEAT", repeat)
        }
        browser?.sendCustomCommand(
            SessionCommand("PLAY_SONG", Bundle.EMPTY),
            commandBundle,
        )
    }

    // --- START OF CHANGE: Functions to be removed or changed later ---
    // These now need to be re-implemented to talk to ExoPlayer directly
    fun playNext(song: Song) {
        val commandBundle = Bundle().apply {
            putParcelable("SONG", song)
        }
        browser?.sendCustomCommand(
            SessionCommand("PLAY_NEXT", Bundle.EMPTY),
            commandBundle,
        )
    }

    fun addSongToQueue(song: Song) {
        val commandBundle = Bundle().apply {
            putParcelable("SONG", song)
        }
        browser?.sendCustomCommand(
            SessionCommand("ADD_TO_QUEUE", Bundle.EMPTY),
            commandBundle
        )
    }

    fun seekTo(position: Long) {
        browser?.seekTo(position)
    }

    fun skipToNext() {
        browser?.seekToNext()
    }

    fun skipToPrevious() {
        browser?.seekToPrevious()
    }

    fun initialize(context: Context) {
        if (browser == null) {
            val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
            viewModelScope.launch {
                val browserFuture = MediaBrowser.Builder(context, sessionToken)
                    .setListener(BrowserListener())
                    .buildAsync()
                browser = browserFuture.await()
                browser?.addListener(PlayerStateListener())
                _playerState.value = browser // Expose the initial state
                _nowPlaying.value = browser?.currentMediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                browser?.currentPosition?.let {
                    _playerPosition.postValue(it)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        stopProgressUpdates()
        browser?.release()
        super.onCleared()
    }

    private inner class BrowserListener : MediaBrowser.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (command.customAction == "QUEUE_STATE_UPDATE") {
                _manualQueueCount.value = args.getInt("MANUAL_QUEUE_COUNT", 0)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private inner class PlayerStateListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // --- START OF NEW PROPERTY ---
            // Whenever the timeline or media item changes, update the player state
            if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                _playerState.value = player
            }
            // --- END OF NEW PROPERTY ---
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _isLoading.value = false
            _nowPlaying.value = mediaItem?.mediaMetadata?.extras?.getParcelable("SONG_METADATA")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _isLoading.value = false
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            stopProgressUpdates()
        }
    }
}
