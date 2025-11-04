package com.example.tunespipe

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.math.abs

@OptIn(UnstableApi::class)
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    // TODO still just have a string for the autoplay strategy.
    //  To show on the "After Queue Empty" header.
    //  Set it with this@MusicPlayerService.autoplayStrategy = strategy
    //  In the same places we used to.

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            availableSessionCommands.add(SessionCommand("PLAY_SONG", Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand("PLAY_NEXT", Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand("ADD_TO_QUEUE", Bundle.EMPTY))
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "PLAY_SONG" -> {
                    val songsToPlay = args.getParcelableArrayList<Song>("SONGS_TO_PLAY")
                    val startIndex = args.getInt("START_INDEX", 0)
                    val shuffle = args.getBoolean("SHUFFLE", false)
                    val repeat = args.getBoolean("REPEAT", false)

                    if (songsToPlay.isNullOrEmpty()) {
                        return@onCustomCommand Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    // --- START OF NEW LOGIC ---
                    serviceScope.launch {
                        // 1. Get the song we need to play right now.
                        val firstSong = songsToPlay[startIndex]

                        // 2. Fetch the URL for ONLY that song.
                        val firstStreamUrl = withContext(Dispatchers.IO) {
                            findBestAudioStreamUrl(firstSong)
                        }

                        if (firstStreamUrl == null) {
                            Log.e(
                                "MusicPlayerService",
                                "Could not get stream for the first song. Aborting."
                            )
                            return@launch
                        }

                        // 3. Create a single, fully resolved MediaItem.
                        val resolvedFirstMediaItem = MediaItem.Builder()
                            .setUri(firstStreamUrl)
                            .setMediaId(firstSong.trackId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(firstSong.trackName)
                                    .setArtist(firstSong.artistName)
                                    .setArtworkUri(android.net.Uri.parse(firstSong.artworkUrl))
                                    .setExtras(Bundle().apply {
                                        putParcelable(
                                            "SONG_METADATA",
                                            firstSong
                                        )
                                    })
                                    .build()
                            )
                            .build()

                        // 4. Update the player on the main thread to play this one song.
                        withContext(Dispatchers.Main) {
                            player.shuffleModeEnabled = shuffle
                            player.repeatMode =
                                if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

                            player.clearMediaItems()
                            player.setMediaItem(resolvedFirstMediaItem) // Use setMediaItem for a single song

                            player.prepare()
                            player.play()

                            // 5. Fire-and-forget a background task to add the rest of the songs.
                            launch {
                                // We pass the full list and the index of the song we already added.
                                resolveAndAddRemainingSongs(songsToPlay, startIndex)
                            }
                        }
                    }
                }
                "PLAY_NEXT" -> {
                    val song = args.getParcelable<Song>("SONG")
                    if (song != null) {
                        serviceScope.launch {
                            val mediaItem = createMediaItemFromSong(song)
                            if (mediaItem != null) {
                                withContext(Dispatchers.Main) {
                                    // Add the song right after the current one
                                    val currentIndex = player.currentMediaItemIndex
                                    player.addMediaItem(currentIndex + 1, mediaItem)
                                }
                            }
                        }
                    }
                }

                "ADD_TO_QUEUE" -> {
                    val song = args.getParcelable<Song>("SONG")
                    if (song != null) {
                        serviceScope.launch {
                            val mediaItem = createMediaItemFromSong(song)
                            if (mediaItem != null) {
                                withContext(Dispatchers.Main) {
                                    // Add the song to the very end of the queue
                                    player.addMediaItem(mediaItem)
                                }
                            }
                        }
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private suspend fun createMediaItemFromSong(song: Song): MediaItem? = withContext(Dispatchers.IO) {
        // Now this block runs on a background thread
        val mediaUri = findBestAudioStreamUrl(song)
        if (mediaUri != null) {
            MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.trackName)
                        .setArtist(song.artistName)
                        .setArtworkUri(android.net.Uri.parse(song.artworkUrl))
                        .setExtras(Bundle().apply { putParcelable("SONG_METADATA", song) })
                        .build()
                )
                .build()
        } else {
            Log.e("MusicPlayerService", "Failed to get stream URL for ${song.trackName}")
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    private suspend fun resolveAndAddRemainingSongs(songs: List<Song>, alreadyAddedIndex: Int) {
        Log.d("MusicPlayerService", "Starting background task to add remaining songs to queue.")

        // Iterate through the original list of songs
        for ((index, song) in songs.withIndex()) {
            // Skip the song that is already in the queue and playing
            if (index == alreadyAddedIndex) {
                continue
            }

            // Fetch the URL for the next song in the list
            val streamUrl = withContext(Dispatchers.IO) {
                findBestAudioStreamUrl(song)
            }

            if (streamUrl != null) {
                val resolvedItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.trackId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.trackName)
                            .setArtist(song.artistName)
                            .setArtworkUri(android.net.Uri.parse(song.artworkUrl))
                            .setExtras(Bundle().apply { putParcelable("SONG_METADATA", song) })
                            .build()
                    )
                    .build()

                // Switch to the main thread briefly to add the new song to the player's queue.
                withContext(Dispatchers.Main) {
                    Log.d("MusicPlayerService", "Adding '${song.trackName}' to the end of the queue.")
                    player.addMediaItem(resolvedItem)
                }
            } else {
                Log.w("MusicPlayerService", "Could not resolve URL for '${song.trackName}'. Skipping.")
            }
        }
        Log.d("MusicPlayerService", "Background queue population complete.")
    }

    private fun findBestAudioStreamUrl(song: Song): String? {
        val searchQuery = "${song.artistName} - ${song.trackName}"
        Log.d("TunesPipe", "############# Starting YouTube search for: $searchQuery")
        val youtubeService = NewPipe.getService(0)
        val searchInfo = SearchInfo.getInfo(
            youtubeService,
            youtubeService.searchQHFactory.fromQuery(searchQuery),
        )

        val itunesDurationSeconds = song.durationMillis / 1000
        val itemsToCheck = searchInfo.relatedItems.take(5).filterIsInstance<StreamInfoItem>()

        Log.d("TunesPipe", "############# Checking top ${itemsToCheck.size} results for duration: $itunesDurationSeconds, explicit: ${song.isExplicit}")

        // 2. Filter by song length (within 2 seconds), but only if it doesn't filter out everything.
        val durationFiltered = itemsToCheck.filter { abs(it.duration - itunesDurationSeconds) <= 2 }
        val candidates = if (durationFiltered.isNotEmpty()) {
            Log.d("TunesPipe", "############# Using duration-filtered list of ${durationFiltered.size} items.")
            durationFiltered
        } else {
            Log.d("TunesPipe", "############# Duration filter produced no results, using original list of ${itemsToCheck.size} items.")
            itemsToCheck
        }

        // 3. Score candidates based on priority words.
        val priorityWords = listOf("lyric", "remaster")
        val explicitWord = "explicit"

        val bestMatch = candidates.maxByOrNull { item ->
            var score = 0
            val title = item.name.lowercase()

            if (priorityWords.any { title.contains(it) }) {
                score += 10  // Lyrics or remasters are usually solid.
            }
            if (song.isExplicit && title.contains(explicitWord)) {
                score += 100  // Getting one explicit is super important. Cleaned are unlistenable.
            }

            Log.d("TunesPipe", "############# Scoring '${item.name}'. Final Score: $score")
            score
        }

        // After scoring, we just need the best match. The position in the original list acts
        // as the tie-breaker because maxByOrNull returns the *first* element with the max value.

        return if (bestMatch != null) {
            Log.d("TunesPipe", "############# Best match chosen: '${bestMatch.name}' with duration ${bestMatch.duration}s.")
            val streamInfo = StreamInfo.getInfo(youtubeService, bestMatch.url)
            streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
        } else {
            Log.d("TunesPipe", "############# No suitable match found after checking all candidates.")
            // Fallback to the closest duration if all scoring fails (though this is unlikely)
            itemsToCheck.minByOrNull { abs(it.duration - itunesDurationSeconds) }?.let { fallbackMatch ->
                Log.d("TunesPipe", "############# Falling back to closest duration: '${fallbackMatch.name}'")
                val streamInfo = StreamInfo.getInfo(youtubeService, fallbackMatch.url)
                streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
