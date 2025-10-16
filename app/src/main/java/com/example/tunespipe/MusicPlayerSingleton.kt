package com.example.tunespipe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs


object MusicPlayerSingleton {
    var exoPlayer: ExoPlayer? = null

    @UnstableApi
    suspend fun playSong(context: Context, song: Song) {
        try {
            // Find the best matching stream URL by checking duration
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

                    // Start the service to enable background playback and notifications
                    context.startService(Intent(context, MusicPlayerService::class.java))
                }
            } else {
                // TODO a toast
                Log.w("TunesPipe", "No suitable audio stream found after checking YouTube results.")
            }
        } catch (e: Exception) {
            // TODO a toast
            Log.e("TunesPipe", "An error occurred during playSongFromSearch", e)
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

                // Check if the duration is within our tolerance, allowing 3 seconds difference.
                if (abs(youtubeDurationSeconds - itunesDurationSeconds) <= 3) {
                    val streamInfo = StreamInfo.getInfo(youtubeService, item.url)
                    return streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
                }
            }
        }
        return null
    }
}
