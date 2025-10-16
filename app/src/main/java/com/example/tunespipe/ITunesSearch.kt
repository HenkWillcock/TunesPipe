package com.example.tunespipe
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class Song(
    val trackName: String,
    val artistName: String,
    val artworkUrl: String,
    val previewUrl: String
)

/**
 * Searches the iTunes API for a given search term.
 *
 * @param searchTerm The term to search for (e.g., "Daft Punk").
 * @return A list of 'Song' objects matching the search, or an empty list if an error occurs.
 */
suspend fun searchITunes(searchTerm: String): List<Song> {
    // This function must run on a background thread.
    // withContext(Dispatchers.IO) ensures this network call doesn't block the UI.
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()


        val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", searchTerm)
            .addQueryParameter("entity", "song")
            .build()

        val request = Request.Builder().url(url).build()

        try {
            Log.d("TunesPipe", "Sending request to url '$url'...")
            val response = client.newCall(request).execute()
            Log.d("TunesPipe", "Response received.")

            if (!response.isSuccessful) {
                return@withContext emptyList()
            }

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonObject = JSONObject(responseBody)
            val resultsArray = jsonObject.optJSONArray("results") ?: return@withContext emptyList()

            // Loop through the JSON results and parse them into a list of 'Song' objects
            val songList = mutableListOf<Song>()
            for (i in 0 until resultsArray.length()) {
                val songObject = resultsArray.getJSONObject(i)
                songList.add(
                    Song(
                        trackName = songObject.optString("trackName", "Unknown Track"),
                        artistName = songObject.optString("artistName", "Unknown Artist"),
                        // Get a larger 600x600 artwork image instead of the default 100x100
                        artworkUrl = songObject.optString("artworkUrl100", "")
                            .replace("100x100bb.jpg", "600x600bb.jpg"),
                        previewUrl = songObject.optString("previewUrl", "")
                    )
                )
            }
            songList // Return the completed list
        } catch (e: Exception) {
            // If anything goes wrong (network issue, JSON parsing error), return an empty list.
            emptyList()
        }
    }
}
