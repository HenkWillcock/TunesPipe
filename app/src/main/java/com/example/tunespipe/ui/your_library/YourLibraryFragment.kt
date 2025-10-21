package com.example.tunespipe.ui.your_library

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.databinding.FragmentYourLibraryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class YourLibraryFragment : Fragment() {

    private var _binding: FragmentYourLibraryBinding? = null
    private val binding get() = _binding!!

    private val yourLibraryViewModel: YourLibraryViewModel by viewModels {
        YourLibraryViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentYourLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textYourLibrary

        yourLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            textView.text = "You have ${playlists.size} playlists."
        }

        // I will add the FAB to the layout file in a moment.
        // For now, let's set up its click listener.
        binding.fabAddPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
        return root
    }

    private fun showCreatePlaylistDialog() {
        val context = requireContext()
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = "Playlist Name"
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("New Playlist")
            .setView(editText)
            .setPositiveButton("Create") { dialog, _ ->
                val playlistName = editText.text.toString()
                if (playlistName.isNotBlank()) {
                    yourLibraryViewModel.createNewPlaylist(playlistName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
