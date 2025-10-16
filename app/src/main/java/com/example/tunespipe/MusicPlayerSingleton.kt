package com.example.tunespipe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs


object MusicPlayerSingleton {
    public var exoPlayer: Player? = null

    @UnstableApi
    suspend fun playSong(context: Context, song: Song) {
        val searchQuery = "${song.artistName} - ${song.trackName}"
        Log.d("TunesPipe", "Starting YouTube search for: $searchQuery")

        try {
            // Find the best matching stream URL by checking duration
            val streamUrl = withContext(Dispatchers.IO) {
                findBestAudioStreamUrl(searchQuery, song.durationMillis)
            }

            if (streamUrl != null) {
                Log.d("TunesPipe", "Match found! Playing stream: $streamUrl")
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()

                context.startService(Intent(context, MusicPlayerService::class.java))
            } else {
                Log.w("TunesPipe", "No suitable audio stream found after checking YouTube results.")
                // Here you could add a Toast or some other UI feedback for the user
            }
        } catch (e: Exception) {
            Log.e("TunesPipe", "An error occurred during playSongFromSearch", e)
        }
    }

    private suspend fun findBestAudioStreamUrl(searchQuery: String, itunesDurationMillis: Long): String? {
        val youtubeService = NewPipe.getService(0)
        val searchInfo = SearchInfo.getInfo(youtubeService, youtubeService.searchQHFactory.fromQuery(searchQuery))

        val itunesDurationSeconds = itunesDurationMillis / 1000
        val DURATION_TOLERANCE_SECONDS = 5 // Allow 5 seconds of difference

        Log.d("TunesPipe", "Target duration: ~$itunesDurationSeconds seconds.")

        // Take the first 100 items from the search results
        val itemsToCheck = searchInfo.relatedItems.take(100)

        for (item in itemsToCheck) {
            if (item is StreamInfoItem) {
                val youtubeDurationSeconds = item.duration
                Log.d("TunesPipe", "Checking '${item.name}' (Duration: $youtubeDurationSeconds s)")

                // Check if the duration is within our tolerance
                if (abs(youtubeDurationSeconds - itunesDurationSeconds) <= DURATION_TOLERANCE_SECONDS) {
                    Log.d("TunesPipe", "SUCCESS: Duration matches. Fetching stream info for '${item.name}'.")
                    // It's a match, get the full stream info to find the audio URL
                    val streamInfo = StreamInfo.getInfo(youtubeService, item.url)
                    // Return the URL of the highest quality audio stream
                    return streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
                }
            }
        }

        // If the loop finishes without finding a match
        return null
    }
}
