package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels // <-- ADD THIS IMPORT
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.Song
import com.example.tunespipe.database.AppDatabase // <-- ADD THIS IMPORT
import com.example.tunespipe.database.Playlist // <-- ADD THIS IMPORT
import com.example.tunespipe.databinding.FragmentSongActionsBinding
import com.example.tunespipe.ui.your_library.YourLibraryViewModel // <-- ADD THIS IMPORT
import com.example.tunespipe.ui.your_library.YourLibraryViewModelFactory // <-- ADD THIS IMPORT
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton // <-- ADD THIS IMPORT
import kotlinx.coroutines.launch

@UnstableApi
class SongActionsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSongActionsBinding? = null
    private val binding get() = _binding!!

    // Use a lazy delegate to ensure song is not null
    private val song: Song by lazy {
        requireArguments().getParcelable(ARG_SONG)!!
    }

    // --- START OF NEW CODE: ViewModel for playlists ---
    private val yourLibraryViewModel: YourLibraryViewModel by viewModels {
        YourLibraryViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao()
        )
    }
    // --- END OF NEW CODE ---

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

        // --- THIS PART IS UNCHANGED AND WILL CONTINUE TO WORK ---
        binding.playNowButton.setOnClickListener {
            val songToPlay = song
            (parentFragment as? SearchFragment)?.songAdapter?.setPlaying(songToPlay)
            dismiss()
            parentFragment?.lifecycleScope?.launch {
                MusicPlayerSingleton.playSong(requireContext(), songToPlay)
            }
        }
        // --- END OF UNCHANGED PART ---

        // --- START OF NEW CODE: Observe playlists and add buttons ---
        yourLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            // Clear any old buttons before adding new ones
            binding.playlistButtonsContainer.removeAllViews()
            // For each playlist, create and add a new button
            playlists.forEach { playlist ->
                addPlaylistButton(playlist)
            }
        }
        // --- END OF NEW CODE ---
    }

    // --- START OF NEW CODE: Helper function to create and add a button ---
    private fun addPlaylistButton(playlist: Playlist) {
        val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Add to ${playlist.name}"
            setOnClickListener {
                yourLibraryViewModel.addSongToPlaylist(song, playlist.id)
                dismiss()
            }
        }
        binding.playlistButtonsContainer.addView(button)
    }
    // --- END OF NEW CODE ---

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
