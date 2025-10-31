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

    // ... companion object is unchanged ...
    companion object {
        const val KEY_CURRENT_SONG = "CURRENTLY_DOWNLOADING_SONG"
    }

    override suspend fun doWork(): Result {
        Log.d("DownloadWorker", "Background download task starting...")
        setProgress(workDataOf(KEY_CURRENT_SONG to null))

        // --- START OF NEW LOGIC ---
        // Run the cleanup process before doing anything else.
        DownloadManager.cleanupFiles(applicationContext)
        // --- END OF NEW LOGIC ---

        val playlistDao = AppDatabase.getDatabase(applicationContext).playlistDao()

        try {
            // ... rest of the download logic is exactly the same ...
            val allPlaylistsWithSongs = playlistDao.getAllPlaylistsWithSongs()
            val allSongsInPlaylists = allPlaylistsWithSongs
                .flatMap { it.songs }
                .distinctBy { it.trackId }

            Log.d("DownloadWorker", "Found ${allSongsInPlaylists.size} unique songs across all playlists.")

            for (song in allSongsInPlaylists) {
                val songFile = DownloadManager.getSongFile(applicationContext, song)
                if (!songFile.exists()) {
                    Log.d("DownloadWorker", "Song '${song.trackName}' not found locally. Starting download.")
                    val songName = "${song.artistName} - ${song.trackName}"
                    setProgress(workDataOf(KEY_CURRENT_SONG to songName))
                    DownloadManager.downloadSong(applicationContext, song)
                } else {
                    Log.d("DownloadWorker", "Song '${song.trackName}' is already downloaded. Skipping.")
                }
            }

            Log.d("DownloadWorker", "Background download task finished successfully.")
            setProgress(workDataOf(KEY_CURRENT_SONG to null))
            return Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Background download task failed: ${e.message}")
            return Result.failure()
        }
    }
}
