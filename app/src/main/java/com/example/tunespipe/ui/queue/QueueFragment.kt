package com.example.tunespipe.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentQueueBinding
import com.example.tunespipe.ui.SongRecyclerViewAdapter
import com.example.tunespipe.ui.search.SongActionsDialogFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(UnstableApi::class)
class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: MusicPlayerViewModel by activityViewModels()
    private lateinit var songAdapter: SongRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Correctly inflate using the generated binding class name for fragment_queue.xml
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use the standard SongRecyclerViewAdapter
        songAdapter = SongRecyclerViewAdapter(emptyList(), playerViewModel) { clickedSong ->
            // When a song in the queue is clicked, it should just play from its position
            // in the player's timeline.
            playerViewModel.browserFlow.value?.let { browser ->
                for (i in 0 until browser.mediaItemCount) {
                    val item = browser.getMediaItemAt(i)
                    if (item.mediaId == clickedSong.trackId) {
                        browser.seekTo(i, 0)
                        browser.play()
                        break
                    }
                }
            }
        }

        binding.songsRecyclerView.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Use the correct lifecycle-aware coroutine scope
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.browserFlow.onEach { browser ->
                    if (browser == null) return@onEach

                    fun updateQueue() {
                        val songsInQueue = mutableListOf<Song>()
                        for (i in 0 until browser.mediaItemCount) {
                            val mediaItem = browser.getMediaItemAt(i)
                            val song = mediaItem.mediaMetadata.extras?.getParcelable<Song>("SONG_METADATA")
                            if (song != null) {
                                songsInQueue.add(song)
                            }
                        }
                        songAdapter.updateSongs(songsInQueue)
                    }

                    // Initial UI update
                    updateQueue()

                    // Set up a listener to update the UI whenever the playlist changes
                    browser.addListener(object : Player.Listener {
                        // onTimelineChanged covers adding, removing, and reordering items.
                        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                            updateQueue()
                        }

                        // onMediaItemTransition covers when the player moves to a different song.
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            // We need to update the adapter to reflect the new "now playing" state
                            updateQueue()
                        }
                    })
                }.launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
