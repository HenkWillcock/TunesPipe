package com.example.tunespipe

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object DownloadManager {

    // Helper function to generate a standardized, safe filename
    private fun getSongFileName(song: Song): String {
        // Sanitize artist and track names to be safe for filenames
        val artist = song.artistName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val title = song.trackName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "$artist - $title.m4a"
    }

    // Public function to get the expected file for a song
    fun getSongFile(context: Context, song: Song): File {
        val musicDir = context.getExternalFilesDir("Music")
        return File(musicDir, getSongFileName(song))
    }

    // --- START OF NEW LOGIC ---
    // Helper function to get the temporary download file
    private fun getTmpFile(context: Context, song: Song): File {
        val musicDir = context.getExternalFilesDir("Music")
        return File(musicDir, "${getSongFileName(song)}.tmp")
    }
    // --- END OF NEW LOGIC ---

    // The main download function
    suspend fun downloadSong(context: Context, song: Song) {
        withContext(Dispatchers.IO) {
            val finalFile = getSongFile(context, song)
            val tmpFile = getTmpFile(context, song)

            if (finalFile.exists()) {
                Log.d("DownloadManager", "Song '${song.trackName}' already downloaded. Skipping.")
                return@withContext
            }

            Log.d("DownloadManager", "Starting download for '${song.trackName}'")
            val streamUrl = findBestAudioStreamUrlForDownload(song)

            if (streamUrl == null) {
                Log.e("DownloadManager", "Could not find a suitable stream URL for ${song.trackName}")
                return@withContext
            }

            try {
                val url = URL(streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream: InputStream = connection.inputStream
                // Download to the temporary file
                val outputStream = FileOutputStream(tmpFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                // --- START OF MODIFIED LOGIC: Rename on success, do nothing on failure ---
                if (!tmpFile.renameTo(finalFile)) {
                    Log.e("DownloadManager", "Failed to rename temp file for '${song.trackName}'")
                } else {
                    Log.d("DownloadManager", "Successfully downloaded and renamed '${song.trackName}'")
                }
                // --- END OF MODIFIED LOGIC ---

            } catch (e: Exception) {
                Log.e("DownloadManager", "Error downloading song: ${e.message}. Partial tmp file may remain.")
                // --- START OF MODIFIED LOGIC: No deletion in the catch block ---
                // The partial tmpFile is intentionally left for a future cleanup process.
                // --- END OF MODIFIED LOGIC ---
            }
        }
    }

    // This is the same YouTube search logic from MusicPlayerService, ensuring consistency
    private fun findBestAudioStreamUrlForDownload(song: Song): String? {
        val searchQuery = "${song.artistName} - ${song.trackName}"
        val youtubeService = NewPipe.getService(0)
        val searchInfo = SearchInfo.getInfo(
            youtubeService,
            youtubeService.searchQHFactory.fromQuery(searchQuery),
        )

        val itunesDurationSeconds = song.durationMillis / 1000
        val itemsToCheck = searchInfo.relatedItems.take(5).filterIsInstance<StreamInfoItem>()
        val durationFiltered = itemsToCheck.filter { abs(it.duration - itunesDurationSeconds) <= 2 }

        val candidates = if (durationFiltered.isNotEmpty()) durationFiltered else itemsToCheck

        val bestMatch = candidates.maxByOrNull { item ->
            var score = 0
            val title = item.name.lowercase()
            if (listOf("lyric", "remaster").any { title.contains(it) }) score += 10
            if (song.isExplicit && title.contains("explicit")) score += 100
            score
        }

        return bestMatch?.let {
            val streamInfo = StreamInfo.getInfo(youtubeService, it.url)
            streamInfo.audioStreams.maxByOrNull { audioStream -> audioStream.averageBitrate }?.content
        }
    }
}
