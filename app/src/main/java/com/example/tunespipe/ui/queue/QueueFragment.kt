package com.example.tunespipe.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tunespipe.AutoplayStrategy
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.R
import com.example.tunespipe.databinding.FragmentQueueBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val playerViewModel: MusicPlayerViewModel by activityViewModels()

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

        viewLifecycleOwner.lifecycleScope.launch {
            // Combine nowPlaying and the (yet to be created) strategy flow
            playerViewModel.nowPlaying.combine(playerViewModel.strategy) { song, strategy ->
                Pair(song, strategy)
            }.collect { (song, strategy) ->
                if (song != null) {
                    binding.queueHeaderText.visibility = View.VISIBLE
                    binding.songTitleText.text = song.trackName
                    binding.artistNameText.text = song.artistName
                    Glide.with(this@QueueFragment)
                        .load(song.artworkUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(binding.artworkImage)

                    binding.strategyText.visibility = View.VISIBLE
                    when (strategy) {
                        is AutoplayStrategy.RepeatOne -> {
                            binding.strategyText.text = "Repeat One"
                            binding.strategyText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one_24, 0, 0, 0)
                        }
                        is AutoplayStrategy.ShufflePlaylist -> {
                            binding.strategyText.text = "Shuffle Playlist"
                            binding.strategyText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle_24, 0, 0, 0)
                        }
                        else -> {
                            binding.strategyText.visibility = View.GONE
                        }
                    }
                } else {
                    binding.queueHeaderText.visibility = View.GONE
                    binding.songTitleText.text = "Nothing is playing."
                    binding.artistNameText.text = ""
                    binding.strategyText.text = ""
                    binding.strategyText.visibility = View.GONE
                    binding.artworkImage.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
