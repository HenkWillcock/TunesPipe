package com.example.tunespipe.ui.playlist_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.databinding.FragmentPlaylistDetailBinding
import com.example.tunespipe.ui.SongRecyclerViewAdapter
import com.example.tunespipe.ui.search.SongActionsDialogFragment

@UnstableApi
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

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

        val songAdapter = SongRecyclerViewAdapter(emptyList()) { clickedSong ->
            val songActionsDialog = SongActionsDialogFragment.newInstance(clickedSong)
            songActionsDialog.show(childFragmentManager, "SongActionsDialog")
        }

        binding.songsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
        }

        viewModel.playlistWithSongs.observe(viewLifecycleOwner) { playlistWithSongs ->
            if (playlistWithSongs != null) {
                songAdapter.updateSongs(playlistWithSongs.songs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
