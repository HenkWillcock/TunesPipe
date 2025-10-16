package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentSongActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

@UnstableApi
class SongActionsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSongActionsBinding? = null
    private val binding get() = _binding!!

    private var song: Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the song passed from SearchFragment
        song = arguments?.getParcelable(ARG_SONG)
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

        binding.songTitleText.text = song?.trackName
        binding.artistNameText.text = song?.artistName

        binding.playNowButton.setOnClickListener {
            song?.let { songToPlay ->
                (parentFragment as? SearchFragment)?.songAdapter?.setPlaying(songToPlay)

                dismiss()

                parentFragment?.lifecycleScope?.launch {
                    MusicPlayerSingleton.playSong(requireContext(), songToPlay)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SONG = "song"

        // A factory method to create a new instance of the dialog with a song
        fun newInstance(song: Song): SongActionsDialogFragment {
            val fragment = SongActionsDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_SONG, song)
            fragment.arguments = args
            return fragment
        }
    }
}
