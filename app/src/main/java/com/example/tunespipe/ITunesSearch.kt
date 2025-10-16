package com.example.tunespipe
import android.util.Log
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress


@Parcelize
data class Song(
    val trackName: String,
    val artistName: String,
    val artworkUrl: String,
    val previewUrl: String,
    val durationMillis: Long,
) : Parcelable


suspend fun searchITunes(searchTerm: String): List<Song> {
    return withContext(Dispatchers.IO) {

        val client = OkHttpClient
            .Builder()
            .dns(
                // It's crucial we use IPv4
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
                    }
                }
            ).build()

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

            val songList = mutableListOf<Song>()
            for (i in 0 until resultsArray.length()) {
                val songObject = resultsArray.getJSONObject(i)
                songList.add(
                    Song(
                        trackName = songObject.optString("trackName", "Unknown Track"),
                        artistName = songObject.optString("artistName", "Unknown Artist"),
                        artworkUrl = songObject.optString("artworkUrl100", "")
                            .replace("100x100bb.jpg", "600x600bb.jpg"),
                        previewUrl = songObject.optString("previewUrl", ""),
                        durationMillis = songObject.optLong("trackTimeMillis", 0),
                    )
                )
            }
            songList
        } catch (e: Exception) {
            emptyList()
        }
    }
}
