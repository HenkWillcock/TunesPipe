package com.example.tunespipe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tunespipe.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.schabi.newpipe.extractor.NewPipe
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

const val NOTIFICATION_CHANNEL_ID = "tunespipe_media_playback"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Get a reference to the new ViewModel
    private val playerViewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_search,
                R.id.navigation_queue,
                R.id.navigation_playlists,
                R.id.navigation_donate
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.playlistDetailFragment) {
                navView.menu.findItem(R.id.navigation_playlists).isChecked = true
            }
        }

        NewPipe.init(DownloaderImpl(HttpClient.instance))
        setupNotificationManager()

        // Initialize the ViewModel, which will connect to the service.
        playerViewModel.initialize(this)

        setupBackgroundDownloader()
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

    private fun setupBackgroundDownloader() {
        // Create a periodic work request that runs roughly every 6 hours.
        // Android may adjust the timing to save battery (e.g., run it when charging/on Wi-Fi).
        val downloadWorkRequest = PeriodicWorkRequestBuilder<DownloadWorker>(6, TimeUnit.HOURS)
            .build()

        // Enqueue the work as unique periodic work.
        // This ensures that only one instance of this worker is scheduled at a time.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PlaylistDownloadWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already scheduled.
            downloadWorkRequest
        )

        Log.d("MainActivity", "Background download worker scheduled.")
    }
}
