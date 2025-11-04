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
                    // Get the list of songs and the starting index from the ViewModel
                    val songsToPlay = args.getParcelableArrayList<Song>("SONGS_TO_PLAY")
                    val shuffle = args.getBoolean("SHUFFLE", false)
                    val repeat = args.getBoolean("REPEAT", false)

                    if (!songsToPlay.isNullOrEmpty()) {
                        // Launch a coroutine to fetch URLs and then update the player on the main thread
                        serviceScope.launch {
                            // This part runs in the background
                            val mediaItems = songsToPlay.mapNotNull { song ->
                                createMediaItemFromSong(song)
                            }

                            // Switch back to the main thread to interact with the player
                            withContext(Dispatchers.Main) {
                                if (mediaItems.isNotEmpty()) {
                                    player.shuffleModeEnabled = shuffle
                                    player.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                                    player.clearMediaItems()
                                    player.addMediaItems(mediaItems)
                                    player.seekTo(startIndex, 0) // Seek to the correct song
                                    player.prepare()
                                    player.play()
                                }
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

    private suspend fun createMediaItemFromSong(song: Song): MediaItem? {
        val mediaUri = findBestAudioStreamUrl(song)
        return if (mediaUri != null) {
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
