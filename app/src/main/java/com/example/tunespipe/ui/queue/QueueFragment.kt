package com.example.tunespipe.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.databinding.FragmentQueueBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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

        queueAdapter = QueueAdapter(emptyList())
        binding.queueRecyclerView.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.nowPlaying
                .combine(playerViewModel.queue) { song, queue ->
                    Pair(song, queue)
                }
                .combine(playerViewModel.strategy) { (song, queue), strategy ->
                    Triple(song, queue, strategy)
                }
                .collect { (song, queue, strategy) ->

                    val queueItems = mutableListOf<QueueItem>()

                    // 1. Add "Now Playing" if it exists
                    if (song != null) {
                        queueItems.add(QueueItem.NowPlaying(song))
                    }

                    // 2. Add "Up Next" header and queued songs if the queue is not empty
                    if (queue.isNotEmpty()) {
                        queueItems.add(QueueItem.Header("Up Next"))
                        queue.forEach { queuedSong ->
                            queueItems.add(QueueItem.QueuedSong(queuedSong))
                        }
                    }

                    // 3. Add the "Once Queue Empty" header and autoplay strategy if it exists
                    if (strategy != null) {
                        // This header appears whether the queue is empty or not, acting as a separator
                        queueItems.add(QueueItem.Header("Once Queue Empty"))
                        queueItems.add(QueueItem.Autoplay(strategy))
                    }

                    queueAdapter.updateItems(queueItems)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
