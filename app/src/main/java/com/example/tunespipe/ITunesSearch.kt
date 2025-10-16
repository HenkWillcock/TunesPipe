package com.example.tunespipe
import android.util.Log
import okhttp3.Dns // <-- Add this import
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address // <-- Add this import
import java.net.InetAddress // <-- Add this import


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
    return withContext(Dispatchers.IO) {

        val ipv4Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
            }
        }

        val client = OkHttpClient.Builder()
            .dns(ipv4Dns) // Tell the client to use our IPv4-only DNS resolver
            .build()

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

            // ... (The rest of the function is exactly the same)
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonObject = JSONObject(responseBody)
            val resultsArray = jsonObject.optJSONArray("results") ?: return@withContext emptyList()

            val songList = mutableListOf<Song>()
            for (i in 0 until resultsArray.length()) {
                val songObject = resultsArray.getJSONObject(i)
                songList.add(
                    Song(
                        trackName = songObject.optString("trackName", "Unknown Track"),
                        artistName = songObject.optString("artistName", "Unknown Artist"),
                        artworkUrl = songObject.optString("artworkUrl100", "")
                            .replace("100x100bb.jpg", "600x600bb.jpg"),
                        previewUrl = songObject.optString("previewUrl", "")
                    )
                )
            }
            songList
        } catch (e: Exception) {
            emptyList()
        }
    }
}
