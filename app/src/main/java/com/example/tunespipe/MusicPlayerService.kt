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

    private var autoplayStrategy: AutoplayStrategy = AutoplayStrategy.RepeatOne
    private var currentSong: Song? = null
    private var songQueue: MutableList<Song> = mutableListOf()

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
            availableSessionCommands.add(SessionCommand("UPDATE_QUEUE", Bundle.EMPTY))
            // --- START OF FIX: Removed PLAY_NEXT_IN_QUEUE ---
            // --- END OF FIX ---
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "UPDATE_QUEUE" -> {
                    val queueSongs = args.getParcelableArrayList<Song>("QUEUE_SONGS")
                    if (queueSongs != null) {
                        this@MusicPlayerService.songQueue = queueSongs.toMutableList()
                    }
                }
                "PLAY_SONG" -> {
                    val song = args.getParcelable<Song>("SONG_TO_PLAY")
                    val strategy = args.getParcelable<AutoplayStrategy>("AUTOPLAY_STRATEGY")
                    val queueSongs = args.getParcelableArrayList<Song>("QUEUE_SONGS")
                    if (song != null && strategy != null) {
                        serviceScope.launch { playSongInternal(song, strategy, queueSongs ?: emptyList()) }
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // --- START OF FIX: Service handles its own queue logic again ---
                if (songQueue.isNotEmpty()) {
                    // There's a song in the queue, let's play it.
                    val nextSong = songQueue.removeAt(0)

                    // Tell the ViewModel to update its UI state for the queue.
                    mediaSession?.broadcastCustomCommand(SessionCommand("UPDATE_QUEUE", Bundle.EMPTY), Bundle().apply {
                        putParcelableArrayList("QUEUE_SONGS", ArrayList(songQueue))
                    })

                    // Play the song, but critically, we keep the original autoplayStrategy.
                    // The queue passed here is the now-shortened queue.
                    serviceScope.launch { playSongInternal(nextSong, autoplayStrategy, songQueue) }
                    return
                }
                // --- END OF FIX ---

                // If we get here, the queue was empty. Resume normal autoplay.
                when (val strategy = autoplayStrategy) {
                    is AutoplayStrategy.RepeatOne -> {
                        player.seekTo(0)
                        player.playWhenReady = true
                    }
                    is AutoplayStrategy.ShufflePlaylist -> {
                        val playlistSongs = strategy.playlistWithSongs.songs
                        val nextSong = playlistSongs.filter { it != currentSong }.randomOrNull()
                        if (nextSong != null) {
                            // Start the next shuffled song with an empty queue.
                            serviceScope.launch { playSongInternal(nextSong, strategy, emptyList()) }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    private suspend fun playSongInternal(song: Song, strategy: AutoplayStrategy, queue: List<Song>) {
        this.currentSong = song
        this.autoplayStrategy = strategy
        this.songQueue = queue.toMutableList()

        // --- START OF MODIFIED LOGIC ---
        val localFile = DownloadManager.getSongFile(this, song)
        val mediaUri: String?

        if (localFile.exists()) {
            Log.d("TunesPipe", "Playing from local file: ${localFile.absolutePath}")
            mediaUri = localFile.toURI().toString()
        } else {
            Log.d("TunesPipe", "Local file not found. Streaming from YouTube.")
            mediaUri = withContext(Dispatchers.IO) { findBestAudioStreamUrl(song) }
        }
        // --- END OF MODIFIED LOGIC ---


        withContext(Dispatchers.Main) {
            // --- START OF MODIFIED LOGIC ---
            if (mediaUri != null) {
                // --- END OF MODIFIED LOGIC ---
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.trackName)
                    .setArtist(song.artistName)
                    .setArtworkUri(android.net.Uri.parse(song.artworkUrl))
                    .setExtras(Bundle().apply { putParcelable("SONG_METADATA", song) })
                    .build()

                val mediaItem = MediaItem.Builder()
                    // --- START OF MODIFIED LOGIC ---
                    .setUri(mediaUri)
                    // --- END OF MODIFIED LOGIC ---
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            } else {
                Log.e("TunesPipe", "Could not find a playable URI for ${song.trackName}. Cannot play.")
            }
        }
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
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
