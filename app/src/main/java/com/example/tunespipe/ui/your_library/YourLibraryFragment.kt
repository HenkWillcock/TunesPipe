package com.example.tunespipe.ui.your_library

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
// --- START OF CHANGE: Import the NavController ---
import androidx.navigation.fragment.findNavController
// --- END OF CHANGE ---
import androidx.recyclerview.widget.LinearLayoutManager
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

        // --- START OF CHANGE: Update adapter instantiation to handle clicks ---
        // 1. Create the adapter, passing in the logic for what happens on click.
        val adapter = PlaylistAdapter { playlist ->
            // When an item is clicked, create the navigation action, passing the playlist's ID.
            val action = YourLibraryFragmentDirections.actionYourLibraryToPlaylistDetail(playlist.id)
            // Use the NavController to perform the navigation.
            findNavController().navigate(action)
        }
        // --- END OF CHANGE ---

        // 2. Set up the RecyclerView
        binding.playlistsRecyclerView.adapter = adapter
        binding.playlistsRecyclerView.layoutManager = LinearLayoutManager(context)

        // 3. Observe the LiveData and submit the new list to the adapter
        yourLibraryViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            playlists?.let {
                adapter.submitList(it)
            }
        }

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
