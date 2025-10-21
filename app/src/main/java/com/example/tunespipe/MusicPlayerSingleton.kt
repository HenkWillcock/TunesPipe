package com.example.tunespipe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player // <-- ADD THIS IMPORT
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs


object MusicPlayerSingleton {
    var exoPlayer: ExoPlayer? = null

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    // --- START OF NEW CODE: Centralized Player Logic ---
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // Logic for what happens when a song ends now lives here, safely.
                Log.d("MusicPlayerSingleton", "Song ended. Repeating.")
                exoPlayer?.seekTo(0)
                exoPlayer?.playWhenReady = true
            }
        }
    }

    /**
     * Initializes the Singleton's player and attaches the central listener.
     * This should be called once when the app starts.
     */
    fun initialize(player: ExoPlayer) {
        if (this.exoPlayer == null) {
            this.exoPlayer = player
            player.addListener(playerListener)
        }
    }
    // --- END OF NEW CODE ---


    @UnstableApi
    suspend fun playSong(context: Context, song: Song) {
        // This existing logic is now safe because the listener won't fight it.
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()

        _nowPlaying.value = song // Show the spinner for the new song

        try {
            val streamUrl = withContext(Dispatchers.IO) {
                findBestAudioStreamUrl(song)
            }

            if (streamUrl != null) {
                Log.d("TunesPipe", "Match found! Playing stream: $streamUrl")
                withContext(Dispatchers.Main) {
                    val mediaItem = MediaItem.fromUri(streamUrl)
                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                    exoPlayer?.play()

                    context.startService(Intent(context, MusicPlayerService::class.java))
                }
            } else {
                Log.w("TunesPipe", "No suitable audio stream found after checking YouTube results.")
                _nowPlaying.value = null // Clear spinner if playing fails
            }
        } catch (e: Exception) {
            Log.e("TunesPipe", "An error occurred during playSongFromSearch", e)
            _nowPlaying.value = null // Clear spinner on error
        }
    }

    private fun findBestAudioStreamUrl(song: Song): String? {
        val searchQuery = "${song.artistName} - ${song.trackName}"
        Log.d("TunesPipe", "Starting YouTube search for: $searchQuery")
        val youtubeService = NewPipe.getService(0)
        val searchInfo = SearchInfo.getInfo(
            youtubeService,
            youtubeService.searchQHFactory.fromQuery(searchQuery),
        )

        val itunesDurationSeconds = song.durationMillis / 1000
        Log.d("TunesPipe", "Target duration: ~$itunesDurationSeconds seconds.")

        val itemsToCheck = searchInfo.relatedItems.take(100)

        for (item in itemsToCheck) {
            if (item is StreamInfoItem) {
                val youtubeDurationSeconds = item.duration
                Log.d("TunesPipe", "Checking '${item.name}' (Duration: $youtubeDurationSeconds s)")

                if (abs(youtubeDurationSeconds - itunesDurationSeconds) <= 3) {
                    val streamInfo = StreamInfo.getInfo(youtubeService, item.url)
                    return streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
                }
            }
        }
        return null
    }
}
