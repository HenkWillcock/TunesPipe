package com.example.tunespipe

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

// A Singleton object to hold our ExoPlayer instance
object MusicPlayer {

    private var exoPlayer: Player? = null

    fun getOrCreatePlayer(context: Context): Player {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build()
        }
        return exoPlayer!!
    }

    suspend fun playSongFromSearch(searchQuery: String) {
        val youtubeService: StreamingService = NewPipe.getService(0)

        val searchInfo = withContext(Dispatchers.IO) {
            val handler = youtubeService.searchQHFactory.fromQuery(
                searchQuery,
//                    listOf("music"),
//                    "",  // TODO
            )
            SearchInfo.getInfo(youtubeService, handler)
        }
        val firstVideo = searchInfo.relatedItems.firstOrNull { it is StreamInfoItem } as? StreamInfoItem
        Log.d("TunesPipe", "First vid: $firstVideo")
        val url = firstVideo?.url
        Log.d("TunesPipe", "Heya mate, here's ya URL: $url")

        val streamInfo = withContext(Dispatchers.IO) {
            StreamInfo.getInfo(youtubeService, url)
        }

        // Log basic info from the successfully fetched stream
        Log.d("TunesPipe", "Successfully fetched stream: ${streamInfo.name}")
        Log.d("TunesPipe", "Uploader: ${streamInfo.uploaderName}")

        // Get the audio stream with the highest bitrate for the best quality
        val audioStream: AudioStream? = streamInfo.audioStreams.maxByOrNull { it.averageBitrate }

        if (audioStream != null) {
            val streamUrl = audioStream.content
            Log.d("TunesPipe", "Found audio stream!")
            Log.d("TunesPipe", "Bitrate: ${audioStream.averageBitrate} kbps")
            Log.d("TunesPipe", "Format: ${audioStream.format}")
            Log.d("TunesPipe", "URL: $streamUrl")

            // 1. Create a MediaItem from the stream URL
            val mediaItem = MediaItem.fromUri(streamUrl)

            // 2. Set the MediaItem on the player
            exoPlayer?.setMediaItem(mediaItem)

            // 3. Prepare the player to start loading the media
            exoPlayer?.prepare()

            // 4. Start playback
            exoPlayer?.play()

        } else {
            Log.e("TunesPipe", "No audio streams found for this video.")
        }
    }
}
