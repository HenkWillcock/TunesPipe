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
import kotlinx.coroutines.Job
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
    private var queuePopulationJob: Job? = null
    private var numberOfManuallyQueuedSongs = 0

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
            availableSessionCommands.add(SessionCommand("GET_QUEUE_STATE", Bundle.EMPTY))
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

                    if (songsToPlay.isNullOrEmpty()) {
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    serviceScope.launch {
                        queuePopulationJob?.cancel()
                        Log.d("MusicPlayerService", "Previous queue population task cancelled.")

                        // 1. Get the song we need to play right now.
                        val firstSong = songsToPlay[startIndex]

                        // 2. Fetch the URL for ONLY that song.
                        val firstStreamUrl = withContext(Dispatchers.IO) {
                            DownloadManager.findBestAudioStreamUrl(
                                this@MusicPlayerService,
                                firstSong,
                            )
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
                            // --- START OF NEW LOGIC ---
                            // Instead of clearing everything, we'll remove only the old auto-queued songs.
                            // The manually queued songs are at indices [currentIndex + 1] to [currentIndex + numberOfManuallyQueuedSongs].
                            // The auto-queued songs are everything after that.
                            val currentIndex = player.currentMediaItemIndex
                            val totalItems = player.mediaItemCount
                            val autoQueueStartIndex = currentIndex + 1 + numberOfManuallyQueuedSongs

                            // Remove items from the end to avoid index shifting issues.
                            if (autoQueueStartIndex < totalItems) {
                                player.removeMediaItems(autoQueueStartIndex, totalItems)
                                Log.d("MusicPlayerService", "Removed old auto-queued songs.")
                            }

                            // Now, add the new song to the end of the manual queue
                            val insertionPoint = currentIndex + 1 + numberOfManuallyQueuedSongs
                            player.addMediaItem(insertionPoint, resolvedFirstMediaItem)

                            // Set player modes for the new content
                            player.shuffleModeEnabled = false
                            player.repeatMode = Player.REPEAT_MODE_OFF

                            // JUMP to the newly added song and play it.
                            player.seekTo(insertionPoint, 0)
                            player.prepare()
                            player.play()

                            // 5. Fire-and-forget a background task to add the rest of the songs.
                            queuePopulationJob = launch {
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
                                    numberOfManuallyQueuedSongs++
                                    broadcastQueueState()
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
                                    val currentIndex = player.currentMediaItemIndex
                                    val insertionPoint = currentIndex + 1 + numberOfManuallyQueuedSongs
                                    player.addMediaItem(insertionPoint, mediaItem)
                                    numberOfManuallyQueuedSongs++
                                    broadcastQueueState()
                                }
                            }
                        }
                    }
                }
                "GET_QUEUE_STATE" -> {
                    val resultBundle = Bundle().apply {
                        putInt("MANUAL_QUEUE_COUNT", numberOfManuallyQueuedSongs)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private suspend fun createMediaItemFromSong(song: Song): MediaItem? = withContext(Dispatchers.IO) {
        // Now this block runs on a background thread
        val mediaUri = DownloadManager.findBestAudioStreamUrl(
            this@MusicPlayerService,
            song,
        )
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

    private fun broadcastQueueState() {
        val command = SessionCommand("QUEUE_STATE_UPDATE", Bundle.EMPTY)
        val args = Bundle().apply {
            putInt("MANUAL_QUEUE_COUNT", numberOfManuallyQueuedSongs)
        }
        mediaSession?.broadcastCustomCommand(command, args)
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        player.addListener(object : Player.Listener {
            // Keep track of the last known index. Initialize with the player's current index.
            private var previousMediaItemIndex: Int = player.currentMediaItemIndex

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newIndex = player.currentMediaItemIndex

                // A song is "consumed" if we move forward to the next adjacent track.
                // This covers both auto-transitions and manual skips forward.
                if (newIndex == previousMediaItemIndex + 1) {
                    if (numberOfManuallyQueuedSongs > 0) {
                        numberOfManuallyQueuedSongs--
                        broadcastQueueState()
                        Log.d("MusicPlayerService", "Consumed a manual song. Count is now: $numberOfManuallyQueuedSongs")
                    }
                }
                // Always update the previous index to the new current one.
                previousMediaItemIndex = newIndex
            }
        })
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
                DownloadManager.findBestAudioStreamUrl(
                    this@MusicPlayerService,
                    song,
                )
            }

            val resolvedItem = createMediaItemFromSong(song)

            if (resolvedItem != null) {
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
