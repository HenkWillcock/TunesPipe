package com.example.tunespipe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tunespipe.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.schabi.newpipe.extractor.NewPipe

const val NOTIFICATION_CHANNEL_ID = "tunespipe_media_playback"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_search, R.id.navigation_your_library, R.id.navigation_donate
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.playlistDetailFragment) {
                navView.menu.findItem(R.id.navigation_your_library).isChecked = true
            }
        }

        NewPipe.init(DownloaderImpl())
        setupNotificationManager()

        // --- START OF CHANGE: Initialize the Singleton correctly ---
        // Create the player instance...
        val player = ExoPlayer.Builder(this.applicationContext).build()
        // ...then pass it to the Singleton to be configured.
        MusicPlayerSingleton.initialize(player)
        // --- END OF CHANGE ---
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupNotificationManager() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "TunesPipe Media Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the currently playing media"
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
