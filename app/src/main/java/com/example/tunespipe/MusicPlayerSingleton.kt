package com.example.tunespipe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs

object MusicPlayerSingleton {
    var exoPlayer: ExoPlayer? = null
    private var applicationContext: Context? = null

    private val _nowPlaying = MutableStateFlow<Song?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private var autoplayStrategy: AutoplayStrategy = AutoplayStrategy.RepeatOne

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                when (val currentStrategy = autoplayStrategy) {
                    is AutoplayStrategy.RepeatOne -> {
                        Log.d("MusicPlayerSingleton", "Song ended. Repeating.")
                        exoPlayer?.seekTo(0)
                        exoPlayer?.playWhenReady = true
                    }
                    is AutoplayStrategy.ShufflePlaylist -> {
                        Log.d("MusicPlayerSingleton", "Song ended. Shuffling playlist.")
                        val currentSong = nowPlaying.value
                        val nextSong = currentStrategy.playlist.filter { it != currentSong }.randomOrNull()

                        if (nextSong != null) {
                            applicationContext?.let { context ->
                                GlobalScope.launch {
                                    playSong(context, nextSong, currentStrategy)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun initialize(context: Context) {
        if (this.exoPlayer == null) {
            this.applicationContext = context.applicationContext
            this.exoPlayer = ExoPlayer.Builder(context).build()
            this.exoPlayer!!.addListener(playerListener)
        }
    }

    @UnstableApi
    suspend fun playSong(
        context: Context,
        song: Song,
        strategy: AutoplayStrategy
    ) {
        // We can be called from any thread, so update the strategy and UI state first.
        autoplayStrategy = strategy
        _nowPlaying.value = song // Show spinner

        // Perform network operation on IO thread
        val streamUrl = try {
            withContext(Dispatchers.IO) {
                findBestAudioStreamUrl(song)
            }
        } catch (e: Exception) {
            Log.e("TunesPipe", "An error occurred during network search", e)
            null
        }

        // --- START OF FIX: Switch to the Main thread for ALL player interactions ---
        withContext(Dispatchers.Main) {
            if (streamUrl != null) {
                Log.d("TunesPipe", "Match found! Playing stream: $streamUrl")

                // Now we are safely on the main thread
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()

                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()

                context.startService(Intent(context, MusicPlayerService::class.java))
            } else {
                Log.w("TunesPipe", "No suitable audio stream found.")
                _nowPlaying.value = null // Clear spinner if playing fails
            }
        }
        // --- END OF FIX ---
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
