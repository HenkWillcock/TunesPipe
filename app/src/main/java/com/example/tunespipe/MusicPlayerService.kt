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
import kotlinx.coroutines.cancel
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
            // --- START OF FIX: Add a new command to request the next song ---
            availableSessionCommands.add(SessionCommand("PLAY_NEXT_IN_QUEUE", Bundle.EMPTY))
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
                if (songQueue.isNotEmpty()) {
                    // Tell the ViewModel to handle playing the next song in the queue
                    mediaSession?.broadcastCustomCommand(SessionCommand("PLAY_NEXT_IN_QUEUE", Bundle.EMPTY), Bundle.EMPTY)
                    return
                }

                when (val strategy = autoplayStrategy) {
                    is AutoplayStrategy.RepeatOne -> {
                        player.seekTo(0)
                        player.playWhenReady = true
                    }
                    is AutoplayStrategy.ShufflePlaylist -> {
                        val playlistSongs = strategy.playlistWithSongs.songs
                        val nextSong = playlistSongs.filter { it != currentSong }.randomOrNull()
                        if (nextSong != null) {
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

        val streamUrl = withContext(Dispatchers.IO) { findBestAudioStreamUrl(song) }

        withContext(Dispatchers.Main) {
            if (streamUrl != null) {
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.trackName)
                    .setArtist(song.artistName)
                    .setArtworkUri(android.net.Uri.parse(song.artworkUrl))
                    .setExtras(Bundle().apply { putParcelable("SONG_METADATA", song) })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    private fun findBestAudioStreamUrl(song: Song): String? {
        val searchQuery = "${song.artistName} - ${song.trackName}"
        Log.d("TunesPipe", "Starting YouTube search for: $searchQuery")
        val youtubeService = NewPipe.getService(0)
        val searchInfo = SearchInfo.getInfo(
            youtubeService,
            youtubeService.searchQHFactory.fromQuery(searchQuery),
        )

        val itunesDurationSeconds = song.durationMillis / 1000
        val itemsToCheck = searchInfo.relatedItems.take(100)

        for (item in itemsToCheck) {
            if (item is StreamInfoItem) {
                if (item.name.contains("live", ignoreCase = true)) {
                    continue
                }

                val youtubeDurationSeconds = item.duration
                if (abs(youtubeDurationSeconds - itunesDurationSeconds) <= 3) {
                    val streamInfo = StreamInfo.getInfo(youtubeService, item.url)
                    return streamInfo.audioStreams.maxByOrNull { it.averageBitrate }?.content
                }
            }
        }
        return null
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
