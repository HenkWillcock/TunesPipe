package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.database.Playlist
import com.example.tunespipe.databinding.FragmentSongActionsBinding
import com.example.tunespipe.ui.your_library.YourLibraryViewModel
import com.example.tunespipe.ui.your_library.YourLibraryViewModelFactory
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

    private val yourLibraryViewModel: YourLibraryViewModel by viewModels {
        YourLibraryViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- START OF UPDATED CODE ---
        // Set text for title and artist
        binding.songTitleText.text = song.trackName
        binding.artistNameText.text = song.artistName

        // Load artwork into the new ImageView using Glide
        Glide.with(this)
            .load(song.artworkUrl)
            .placeholder(R.drawable.ic_launcher_foreground) // Optional: a placeholder image
            .into(binding.artworkImage)
        // --- END OF UPDATED CODE ---

        binding.playNowButton.setOnClickListener {
            dismiss()
            parentFragment?.lifecycleScope?.launch {
                MusicPlayerSingleton.playSong(requireContext(), song)
            }
        }

        yourLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            binding.playlistButtonsContainer.removeAllViews()
            playlists.forEach { playlist ->
                addPlaylistButton(playlist)
            }
        }
    }

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
