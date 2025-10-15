package com.example.tunespipe

import android.app.NotificationManager
import android.os.Bundle
import android.app.NotificationChannel
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tunespipe.databinding.ActivityMainBinding
import org.schabi.newpipe.extractor.NewPipe

const val NOTIFICATION_CHANNEL_ID = "tunespipe_media_playback"

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
                R.id.navigation_search, R.id.navigation_your_library, R.id.navigation_donate
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Must be called on startup.
        NewPipe.init(DownloaderImpl())

        // --- Add the Notification Channel creation logic ---
        val name = "TunesPipe Media Playback"
        val descriptionText = "Shows the currently playing media"
        val importance = NotificationManager.IMPORTANCE_LOW // Use LOW to prevent sound
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        MusicPlayerSingleton.exoPlayer = ExoPlayer.Builder(this.applicationContext).build()
    }
}
