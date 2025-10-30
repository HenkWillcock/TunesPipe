package com.example.tunespipe

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tunespipe.database.AppDatabase

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DownloadWorker", "Background download task starting...")
        val playlistDao = AppDatabase.getDatabase(applicationContext).playlistDao()

        try {
            // Fetch all playlists with their songs
            val allPlaylistsWithSongs = playlistDao.getAllPlaylistsWithSongs()

            // Flatten the list to get all unique songs across all playlists
            val allSongsInPlaylists = allPlaylistsWithSongs
                .flatMap { it.songs }
                .distinctBy { it.trackId }

            Log.d("DownloadWorker", "Found ${allSongsInPlaylists.size} unique songs across all playlists.")

            for (song in allSongsInPlaylists) {
                val songFile = DownloadManager.getSongFile(applicationContext, song)
                if (!songFile.exists()) {
                    Log.d("DownloadWorker", "Song '${song.trackName}' not found locally. Starting download.")
                    // DownloadManager.downloadSong is already a suspend function,
                    // so it integrates perfectly here.
                    DownloadManager.downloadSong(applicationContext, song)
                    // We download one song at a time to be respectful of network and battery.
                    // The worker will run again later to get the next one.
                } else {
                    Log.d("DownloadWorker", "Song '${song.trackName}' is already downloaded. Skipping.")
                }
            }

            Log.d("DownloadWorker", "Background download task finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Background download task failed: ${e.message}")
            return Result.failure()
        }
    }
}
