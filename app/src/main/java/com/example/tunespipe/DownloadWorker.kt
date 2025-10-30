package com.example.tunespipe
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.tunespipe.database.AppDatabase


class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // --- START OF NEW CODE: Companion object for keys ---
    companion object {
        const val KEY_CURRENT_SONG = "CURRENTLY_DOWNLOADING_SONG"
    }
    // --- END OF NEW CODE ---

    override suspend fun doWork(): Result {
        Log.d("DownloadWorker", "Background download task starting...")
        // --- START OF NEW CODE: Clear progress at the start ---
        // Set initial progress to indicate nothing is being downloaded yet
        setProgress(workDataOf(KEY_CURRENT_SONG to null))
        // --- END OF NEW CODE ---
        val playlistDao = AppDatabase.getDatabase(applicationContext).playlistDao()

        try {
            val allPlaylistsWithSongs = playlistDao.getAllPlaylistsWithSongs()
            val allSongsInPlaylists = allPlaylistsWithSongs
                .flatMap { it.songs }
                .distinctBy { it.trackId }

            Log.d("DownloadWorker", "Found ${allSongsInPlaylists.size} unique songs across all playlists.")

            for (song in allSongsInPlaylists) {
                val songFile = DownloadManager.getSongFile(applicationContext, song)
                if (!songFile.exists()) {
                    Log.d("DownloadWorker", "Song '${song.trackName}' not found locally. Starting download.")

                    // --- START OF NEW CODE: Report current song ---
                    val songName = "${song.artistName} - ${song.trackName}"
                    setProgress(workDataOf(KEY_CURRENT_SONG to songName))
                    // --- END OF NEW CODE ---

                    DownloadManager.downloadSong(applicationContext, song)
                    // We download one song at a time
                } else {
                    Log.d("DownloadWorker", "Song '${song.trackName}' is already downloaded. Skipping.")
                }
            }

            Log.d("DownloadWorker", "Background download task finished successfully.")
            // --- START OF NEW CODE: Clear progress at the end ---
            setProgress(workDataOf(KEY_CURRENT_SONG to null))
            // --- END OF NEW CODE ---
            return Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Background download task failed: ${e.message}")
            return Result.failure()
        }
    }
}
