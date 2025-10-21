package com.example.tunespipe.ui.playlist_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.R // Make sure R is imported
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.databinding.FragmentPlaylistDetailBinding
import com.example.tunespipe.ui.SongRecyclerViewAdapter
import com.example.tunespipe.ui.search.SongActionsDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    private val args: PlaylistDetailFragmentArgs by navArgs()

    public val viewModel: PlaylistDetailViewModel by viewModels {
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

        setupMenu() // Call the new menu setup method

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

    // --- START OF NEW CODE ---
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.playlist_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_delete_playlist -> {
                        showConfirmDeleteDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showConfirmDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '${args.playlistName}'? This cannot be undone.")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Confirm") { dialog, _ ->
                viewModel.deletePlaylist()
                // Navigate back to the library screen after deletion
                findNavController().popBackStack()
                dialog.dismiss()
            }
            .show()
    }
    // --- END OF NEW CODE ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
