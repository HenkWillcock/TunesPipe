package com.example.tunespipe.ui.playlist_detail

import android.os.Bundle
import androidx.lifecycle.observe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.databinding.FragmentPlaylistDetailBinding
import com.example.tunespipe.ui.SongRecyclerView
import kotlinx.coroutines.launch

@UnstableApi
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    // Retrieve arguments passed from the navigation action
    private val args: PlaylistDetailFragmentArgs by navArgs()

    private val viewModel: PlaylistDetailViewModel by viewModels {
        PlaylistDetailViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao(),
            args.playlistId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- START OF CORRECTION ---
        // Initialize the adapter with an empty list and an updated click handler.
        val songAdapter = SongRecyclerView(emptyList()) { clickedSong ->
            // When a song is clicked, call the global play function.
            // The adapter will automatically react to the state change and show the spinner.
            lifecycleScope.launch {
                MusicPlayerSingleton.playSong(requireContext(), clickedSong)
            }
        }
        // --- END OF CORRECTION ---

        binding.songsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
        }

        // Observe the data from the ViewModel
        viewModel.playlistWithSongs.observe(viewLifecycleOwner) { playlistWithSongs ->
            if (playlistWithSongs != null) {
                // Set the activity title to the playlist name
                activity?.title = playlistWithSongs.playlist.name
                // Update the adapter with the list of songs
                songAdapter.updateSongs(playlistWithSongs.songs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the title when leaving the screen
        activity?.title = "Your Library"
        _binding = null
    }
}
