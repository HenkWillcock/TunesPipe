package com.example.tunespipe

import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs

@Parcelize
@Entity(tableName = "songs") // Tell Room this is a database table
data class Song(
    @PrimaryKey val trackId: String, // Mark trackId as the unique Primary Key
    val trackName: String,
    val artistName: String,
    val artworkUrl: String,
    val previewUrl: String?, // Make this nullable to handle cases where it's missing
    val durationMillis: Long,
    val isExplicit: Boolean,
) : Parcelable

suspend fun searchITunes(searchTerm: String): List<Song> {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Get the YouTube Music service
            val ytMusicService = NewPipe.getService(1)

            // 2. Build the search query and perform the search
            val searchInfo = SearchInfo.getInfo(
                ytMusicService,
                ytMusicService.searchQHFactory.fromQuery(searchTerm),
            )
            Log.d("YouTubeSearch", "Searching YT Music for: $searchTerm. Found ${searchInfo.relatedItems.size} initial results.")

            // 3. Map the raw results to our Song data class
            val songs = searchInfo.relatedItems.mapNotNull { item ->
                if (item is StreamInfoItem) {
                    val videoId = item.url.substringAfter("v=")
                    Song(
                        trackId = videoId,
                        trackName = item.name ?: "Unknown Track",
                        artistName = item.uploaderName ?: "Unknown Artist",
                        artworkUrl = item.thumbnails.lastOrNull()?.url ?: "",                        previewUrl = null,
                        durationMillis = (item.duration ?: 0) * 1000,
                        isExplicit = false // No reliable explicit flag from YT
                    )
                } else {
                    null
                }
            }

            Log.d("YouTubeSearch", "Mapped to ${songs.size} Song objects.")
            songs
        } catch (e: Exception) {
            Log.e("YouTubeSearch", "Failed to search YouTube Music", e)
            emptyList<Song>()
        }
    }
}
