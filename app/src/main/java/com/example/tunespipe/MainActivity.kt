package com.example.tunespipe

import android.os.Bundle
import androidx.activity.result.launch
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
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import com.example.tunespipe.DownloaderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.format
import kotlin.text.maxByOrNull

import android.util.Log
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo

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
        val youtubeService: StreamingService = NewPipe.getService(0)

        // FIXME still crashing the thing.
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
                val streamUrl = audioStream.url
                Log.d("TunesPipe", "Found audio stream!")
                Log.d("TunesPipe", "Bitrate: ${audioStream.averageBitrate} kbps")
                Log.d("TunesPipe", "Format: ${audioStream.format}")
                Log.d("TunesPipe", "URL: $streamUrl")
            } else {
                Log.e("TunesPipe", "No audio streams found for this video.")
            }
        }
    }
}
