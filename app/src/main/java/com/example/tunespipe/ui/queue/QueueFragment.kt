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

        // Setup the RecyclerView and Adapter
        queueAdapter = QueueAdapter(emptyList())
        binding.queueRecyclerView.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Combine the nowPlaying and strategy flows to update the UI
            playerViewModel.nowPlaying.combine(playerViewModel.strategy) { song, strategy ->
                Pair(song, strategy)
            }.collect { (song, strategy) ->

                val queueItems = mutableListOf<QueueItem>()

                // If there's a song playing, add it as the first item
                if (song != null) {
                    queueItems.add(QueueItem.NowPlaying(song))
                }

                // If there's a strategy, add it as the last item
                if (strategy != null) {
                    queueItems.add(QueueItem.Autoplay(strategy))
                }

                if (queueItems.isEmpty()) {
                    // Handle the empty case if you want, e.g., show a message
                }

                // Update the adapter with the new list
                queueAdapter.updateItems(queueItems)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
