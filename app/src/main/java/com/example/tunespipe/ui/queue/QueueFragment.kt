package com.example.tunespipe.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentQueueBinding
import kotlinx.coroutines.launch
import android.util.Log


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

        fun updateDisplay() {
            val player = playerViewModel.playerState.value
            val count = playerViewModel.manualQueueCount.value
            updateQueueDisplay(player, count)
        }

        // Observe the player state (the queue itself) for changes.
        playerViewModel.playerState.observe(viewLifecycleOwner) {
            updateDisplay()
        }

        // Observe the manual queue count for changes.
        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.manualQueueCount.collect {
                updateDisplay()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.refreshSignal.collect {
                // When the signal is received, it's our cue to redraw the UI.
                updateDisplay()
            }
        }
    }
    private fun updateQueueDisplay(player: Player?, manualQueueCount: Int) {
        if (player == null || player.mediaItemCount == 0) {
            queueAdapter.updateItems(emptyList())
            return
        }

        val queueItems = mutableListOf<QueueItem>()
        val nowPlayingSong = player.currentMediaItem?.mediaMetadata?.extras?.getParcelable<Song>("SONG_METADATA") as? Song

        // 1. Add "Now Playing"
        if (nowPlayingSong != null) {
            queueItems.add(QueueItem.NowPlaying(nowPlayingSong))
        }

        val totalItems = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex

        // 2. Build the "Up Next" (Manually Queued) list
        // The manually queued songs are the next [manualQueueCount] items after the current one.
        val upNextStartIndex = currentIndex + 1
        // The end index is exclusive, so we add the count to the start index.
        val upNextEndIndex = upNextStartIndex + manualQueueCount

        Log.d("QueueFragment", "############## !!!!!! manualQueueCount: $manualQueueCount")


        if (manualQueueCount > 0 && upNextStartIndex < totalItems) {
            queueItems.add(QueueItem.Header("Up Next"))
            // Loop from the start of "Up Next" to the calculated end, but don't go past the end of the whole queue.
            for (i in upNextStartIndex until minOf(upNextEndIndex, totalItems)) {
                val mediaItem = player.getMediaItemAt(i)
                mediaItem.mediaMetadata.extras?.getParcelable<Song>("SONG_METADATA")?.let { song ->
                    queueItems.add(QueueItem.QueuedSong(song))
                }
            }
        }
        queueItems.add(QueueItem.Header("Then, Auto Queue"))

        if (totalItems > upNextEndIndex) {
            for (i in upNextEndIndex until totalItems) {
                val mediaItem = player.getMediaItemAt(i)
                mediaItem.mediaMetadata.extras?.getParcelable<Song>("SONG_METADATA")?.let { song ->
                    queueItems.add(QueueItem.QueuedSong(song))
                }
            }
        }

        queueAdapter.updateItems(queueItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
