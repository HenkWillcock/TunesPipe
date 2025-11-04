package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.example.tunespipe.AutoplayStrategy
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
import kotlinx.coroutines.launch // Import launch

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

        binding.playNowButton.setOnClickListener {
            dismiss()

            val songsToPlay: List<Song>
            val startIndex: Int

            if (parentFragment is PlaylistDetailFragment) {
                // If we are in a playlist, the queue is the whole playlist
                val playlistFragment = parentFragment as PlaylistDetailFragment
                val playlistWithSongs = playlistFragment.viewModel.playlistWithSongs.value
                songsToPlay = playlistWithSongs?.songs ?: listOf(song)
                // Find the index of the song we clicked
                startIndex = songsToPlay.indexOf(song).coerceAtLeast(0)
            } else {
                // If we are not in a playlist (e.g., search), the queue is just the one song
                songsToPlay = listOf(song)
                startIndex = 0
            }

            val shuffle = false // For now, we are not shuffling when playing from a list
            val repeat = true   // We want the playlist/song to repeat
            playerViewModel.playSong(songsToPlay, startIndex, shuffle, repeat)
        }

        binding.playNextButton.setOnClickListener {
            playerViewModel.playNext(song)
            dismiss()
        }

        binding.addToQueueButton.setOnClickListener {
            playerViewModel.addSongToQueue(song)
            dismiss()
        }

        playlistsViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.playlistButtonsContainer.removeAllViews()
            // --- START OF MODIFIED LOGIC ---
            // Use lifecycleScope to call suspend functions
            lifecycleScope.launch {
                playlists.forEach { playlist ->
                    // Check if the song is already in this playlist
                    val songIsInPlaylist = playlistsViewModel.isSongInPlaylist(song.trackId, playlist.id)
                    // Create the appropriate button
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
                // Set icon for remove action
                setIconResource(R.drawable.ic_remove_24)
                setOnClickListener {
                    playlistsViewModel.removeSongFromPlaylist(song.trackId, playlist.id)
                    // Refresh the dialog or dismiss it
                    dismiss()
                }
            } else {
                text = "Add to ${playlist.name}"
                // Set icon for add action
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
