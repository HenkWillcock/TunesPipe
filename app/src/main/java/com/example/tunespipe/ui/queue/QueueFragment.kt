package com.example.tunespipe.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentQueueBinding


class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: MusicPlayerViewModel by activityViewModels()
    private lateinit var queueAdapter: QueueAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pass the playerViewModel to the adapter's constructor
        queueAdapter = QueueAdapter(emptyList())
        binding.queueRecyclerView.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // --- START OF NEW, CORRECTED LOGIC ---
        // The player's state is the source of truth for the queue.
        playerViewModel.playerState.observe(viewLifecycleOwner) { player ->
            if (player == null) return@observe

            val queueItems = mutableListOf<QueueItem>()
            val nowPlayingSong = player.currentMediaItem?.mediaMetadata?.extras?.getParcelable<Song>("SONG_METADATA")

            // 1. Add "Now Playing" item if a song is loaded
            if (nowPlayingSong != null) {
                queueItems.add(QueueItem.NowPlaying(nowPlayingSong))
            }

            // 2. Build the "Up Next" list from the rest of the player's timeline
            val upNextSongs = mutableListOf<Song>()
            if (player.mediaItemCount > 1) {
                // Start from the item *after* the current one
                for (i in player.currentMediaItemIndex + 1 until player.mediaItemCount) {
                    val mediaItem = player.getMediaItemAt(i)
                    val song = mediaItem.mediaMetadata.extras?.getParcelable<Song>("SONG_METADATA")
                    if (song != null) {
                        upNextSongs.add(song)
                    }
                }
            }

            if (upNextSongs.isNotEmpty()) {
                queueItems.add(QueueItem.Header("Up Next"))
                upNextSongs.forEach { queuedSong ->
                    queueItems.add(QueueItem.QueuedSong(queuedSong))
                }
            }

            // 3. Add the "Once Queue Empty" header and autoplay strategy
            // We will add a placeholder for now and connect it to the service later
            val strategyText = when {
                player.shuffleModeEnabled -> "Shuffle"
                player.repeatMode == Player.REPEAT_MODE_ALL -> "Repeat"
                else -> "Play Once"
            }
            // TODO: We will replace this simple text with a proper AutoplayStrategy object later.
            queueItems.add(QueueItem.Header("Once Queue Empty"))
            // For now, let's just display the text directly. We'll need a new QueueItem or modify the existing one.
            // As a temporary fix, let's just use the Header item to show it.
            queueItems.add(QueueItem.Header("Autoplay: $strategyText"))


            queueAdapter.updateItems(queueItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
