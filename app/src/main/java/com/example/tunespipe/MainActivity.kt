package com.example.tunespipe

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tunespipe.databinding.ActivityMainBinding
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.stream.StreamInfoItem
//import org.schabi.newpipe.extractor.downloader.Downloader
//import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.util.Log
import androidx.media3.common.MediaItem
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.search.SearchInfo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_browse, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        NewPipe.init(DownloaderImpl())
        val exoPlayer: ExoPlayer = ExoPlayer.Builder(this).build()
        val youtubeService: StreamingService = NewPipe.getService(0)

        // TODO, write a function that reliably finds the song with the NewPipeExtractor
        //  using only fields included in the iTunes lookup:
        //  EXAMPLE: https://itunes.apple.com/search?term=jack+johnson

        // Once I can do that, everything else is just building an interface for the
        // iTunes API. Playlists, Radio, Jams, etc.
        lifecycleScope.launch {
            val streamInfo = withContext(Dispatchers.IO) {
                StreamInfo.getInfo(youtubeService, "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            }

            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            Log.d("TunesPipe", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")

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
                exoPlayer.setMediaItem(mediaItem)

                // 3. Prepare the player to start loading the media
                exoPlayer.prepare()

                // 4. Start playback
                exoPlayer.play()

            } else {
                Log.e("TunesPipe", "No audio streams found for this video.")
            }

            val search = youtubeService.getSearchExtractor(
                "Never gonna give you up",
                listOf("music"),
                "",  // TODO
            )
//            val searchInfo = withContext(Dispatchers.IO) {
//                SearchInfo.getInfo(youtubeService, "rick astley never gonna give you up")
//            }
            withContext(Dispatchers.IO) {
                search.fetchPage()
                val items = search.initialPage.items
                val firstVideo = items.firstOrNull { it is StreamInfoItem } as? StreamInfoItem
                val url = firstVideo?.url
                Log.d("TunesPipe", "Heya mate, here's ya URL: $url")
            }
        }
    }
}
