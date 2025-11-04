package com.example.tunespipe

import android.content.Intent // Make sure this import is present
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

    // --- REFACTOR: These are no longer needed. The player is the source of truth. ---
    // private var autoplayStrategy: AutoplayStrategy = AutoplayStrategy.RepeatOne
    // private var currentSong: Song? = null
    // private var songQueue: MutableList<Song> = mutableListOf()
    // ---

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            // --- REFACTOR: New custom command ---
            availableSessionCommands.add(SessionCommand("SET_PLAYLIST", Bundle.EMPTY))
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
                // --- REFACTOR: Handle the new playlist command ---
                "SET_PLAYLIST" -> {
                    val songs = args.getParcelableArrayList<Song>("SONGS")
                    val startIndex = args.getInt("START_INDEX", 0)
                    val shuffle = args.getBoolean("SHUFFLE", false)

                    if (songs != null) {
                        serviceScope.launch { setPlaylistAndPlay(songs, startIndex, shuffle) }
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private val playerListener = object : Player.Listener {
        // --- REFACTOR: This is the new core of our lazy-loading logic ---
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return

            // If the item doesn't have a real URI, it's a placeholder we need to resolve.
            if (mediaItem.localConfiguration?.uri == null) {
                val song = mediaItem.mediaMetadata.extras?.getParcelable<Song>("SONG_METADATA")
                if (song != null) {
                    Log.d("MusicPlayerService", "Transition to unresolved song: ${song.trackName}")
                    serviceScope.launch { resolveAndPlay(song, player.currentMediaItemIndex) }
                }
            }
        }
    }

    // --- REFACTOR: playNextSong() is now deleted. ---

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        // --- REFACTOR: No ForwardingPlayer needed. Just the real player. ---
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    private suspend fun setPlaylistAndPlay(songs: List<Song>, startIndex: Int, shuffle: Boolean) {
        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.trackName)
                .setArtist(song.artistName)
                .setArtworkUri(android.net.Uri.parse(song.artworkUrl))
                .setExtras(Bundle().apply { putParcelable("SONG_METADATA", song) })
                .build()

            MediaItem.Builder()
                .setMediaId(song.trackId)
                .setMediaMetadata(metadata)
                .build()
        }

        player.shuffleModeEnabled = shuffle
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
        player.play()
    }

    private suspend fun resolveAndPlay(song: Song, windowIndex: Int) {
        val playableUri = findPlayableUri(song)

        if (playableUri != null) {
            val originalMediaItem = player.getMediaItemAt(windowIndex)
            val newMediaItem = originalMediaItem.buildUpon().setUri(playableUri).build()

            // Replace the placeholder item with the real, resolved one.
            player.removeMediaItem(windowIndex)
            player.addMediaItem(windowIndex, newMediaItem)
            // The player automatically continues, but we ensure play state just in case.
            player.play()
        } else {
            Log.e("MusicPlayerService", "Could not resolve URI for ${song.trackName}. Skipping.")
            // Just remove the item, the player will automatically move to the next one.
            player.removeMediaItem(windowIndex)
        }
    }

    private suspend fun findPlayableUri(song: Song): String? {
        val localFile = DownloadManager.getSongFile(this, song)
        return if (localFile.exists()) {
            Log.d("MusicPlayerService", "Playing from local file: ${localFile.absolutePath}")
            localFile.toURI().toString()
        } else {
            Log.d("MusicPlayerService", "Local file not found. Streaming from YouTube.")
            withContext(Dispatchers.IO) { findBestAudioStreamUrl(song) }
        }
    }

    // --- REFACTOR: The old `playSongInternal` is GONE. `findBestAudioStreamUrl` remains unchanged. ---
    private fun findBestAudioStreamUrl(song: Song): String? {
        // ... this function's content is exactly the same as before ...
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

        val durationFiltered = itemsToCheck.filter { abs(it.duration - itunesDurationSeconds) <= 2 }
        val candidates = if (durationFiltered.isNotEmpty()) {
            Log.d("TunesPipe", "############# Using duration-filtered list of ${durationFiltered.size} items.")
            durationFiltered
        } else {
            Log.d("TunesPipe", "############# Duration filter produced no results, using original list of ${itemsToCheck.size} items.")
            itemsToCheck
        }

        val priorityWords = listOf("lyric", "remaster")
        val explicitWord = "explicit"

        val bestMatch = candidates.maxByOrNull { item ->
            var score = 0
            val title = item.name.lowercase()

            if (priorityWords.any { title.contains(it) }) {
                score += 10
            }
            if (song.isExplicit && title.contains(explicitWord)) {
                score += 100
            }

            Log.d("TunesPipe", "############# Scoring '${item.name}'. Final Score: $score")
            score
        }

        return if (bestMatch != null) {
            Log.d("TunesPipe", "############# Best match chosen: '${bestMatch.name}' with duration ${bestMatch.duration}s.")
            val streamInfo = StreamInfo.getInfo(youtubeService, bestMatch.url)
            streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
        } else {
            Log.d("TunesPipe", "############# No suitable match found after checking all candidates.")
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
