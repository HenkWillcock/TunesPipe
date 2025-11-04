package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.database.Playlist
import com.example.tunespipe.databinding.FragmentSongActionsBinding
import com.example.tunespipe.ui.playlist_detail.PlaylistDetailFragment
import com.example.tunespipe.ui.playlists.PlaylistsViewModel
import com.example.tunespipe.ui.playlists.PlaylistsViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

@UnstableApi
class SongActionsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSongActionsBinding? = null
    private val binding get() = _binding!!

    private val song: Song by lazy {
        requireArguments().getParcelable(ARG_SONG)!!
    }

    private val playlistsViewModel: PlaylistsViewModel by viewModels {
        PlaylistsViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao()
        )
    }

    private val playerViewModel: MusicPlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.songTitleText.text = song.trackName
        binding.artistNameText.text = song.artistName

        Glide.with(this)
            .load(song.artworkUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(binding.artworkImage)

        // --- REFACTOR: Update all button click listeners ---
        binding.playNowButton.setOnClickListener {
            dismiss()
            // Determine the playlist context
            val parent = parentFragment
            when {
                // Case 1: We are in a playlist detail view
                parent is PlaylistDetailFragment -> {
                    val playlistSongs = parent.viewModel.playlistWithSongs.value?.songs ?: listOf(song)
                    val startIndex = playlistSongs.indexOf(song).coerceAtLeast(0)
                    // Play the song within the context of the playlist, with shuffle enabled
                    playerViewModel.playSongFromPlaylist(playlistSongs, startIndex, shuffle = true)
                }
                // Case 2: We are in search results or elsewhere
                else -> {
                    // Play just this single song, with no shuffle
                    playerViewModel.playSongFromPlaylist(listOf(song), 0, shuffle = false)
                }
            }
        }

        binding.playNextButton.setOnClickListener {
            playerViewModel.playNext(song)
            dismiss()
        }

        binding.addToQueueButton.setOnClickListener {
            playerViewModel.addToQueue(song)
            dismiss()
        }
        // --- END OF REFACTOR ---

        playlistsViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.playlistButtonsContainer.removeAllViews()
            lifecycleScope.launch {
                playlists.forEach { playlist ->
                    val songIsInPlaylist = playlistsViewModel.isSongInPlaylist(song.trackId, playlist.id)
                    addPlaylistButton(playlist, songIsInPlaylist)
                }
            }
        }
    }

    private fun addPlaylistButton(playlist: Playlist, songIsInPlaylist: Boolean) {
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle,
        ).apply {
            if (songIsInPlaylist) {
                text = "Remove from ${playlist.name}"
                setIconResource(R.drawable.ic_remove_24)
                setOnClickListener {
                    playlistsViewModel.removeSongFromPlaylist(song.trackId, playlist.id)
                    dismiss()
                }
            } else {
                text = "Add to ${playlist.name}"
                setIconResource(R.drawable.ic_add_24)
                setOnClickListener {
                    playlistsViewModel.addSongToPlaylist(song, playlist.id)
                    dismiss()
                }
            }
        }
        binding.playlistButtonsContainer.addView(button)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SONG = "song"

        fun newInstance(song: Song): SongActionsDialogFragment {
            return SongActionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SONG, song)
                }
            }
        }
    }
}
