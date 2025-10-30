package com.example.tunespipe.ui.playlists

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.example.tunespipe.DownloadWorker
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.databinding.FragmentPlaylistsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private val playlistsViewModel: PlaylistsViewModel by viewModels {
        PlaylistsViewModelFactory(
            AppDatabase.getDatabase(requireContext()).playlistDao()
        )
    }

    // Add the new ViewModel
    private val downloadStatusViewModel: DownloadStatusViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlaylistAdapter { playlist ->
            val action = PlaylistsFragmentDirections.actionPlaylistsToPlaylistDetail(
                playlistId = playlist.id,
                playlistName = playlist.name
            )
            findNavController().navigate(action)
        }

        binding.playlistsRecyclerView.adapter = adapter
        binding.playlistsRecyclerView.layoutManager = LinearLayoutManager(context)

        binding.fabAddPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }

        playlistsViewModel.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            playlists?.let {
                adapter.submitList(it)
                // Refresh download stats whenever the playlists change
                downloadStatusViewModel.updateDownloadStats()
            }
        }

        // Set up the new observers for download status
        setupDownloadStatusObservers()
    }

    private fun setupDownloadStatusObservers() {
        downloadStatusViewModel.downloadedCount.observe(viewLifecycleOwner) { count ->
            binding.downloadedCountText.text = "Downloaded Songs: $count"
        }

        downloadStatusViewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            binding.pendingCountText.text = "Pending Downloads: $count"
        }

        downloadStatusViewModel.downloadWorkerInfo.observe(viewLifecycleOwner) { workInfos ->
            val workerInfo = workInfos?.firstOrNull()
            if (workerInfo != null && workerInfo.state == WorkInfo.State.RUNNING) {
                val currentSong = workerInfo.progress.getString(DownloadWorker.KEY_CURRENT_SONG)
                if (!currentSong.isNullOrEmpty()) {
                    binding.currentlyDownloadingText.text = "Downloading: $currentSong"
                    binding.currentlyDownloadingText.isVisible = true
                } else {
                    binding.currentlyDownloadingText.isVisible = false
                }
            } else {
                binding.currentlyDownloadingText.isVisible = false
                // Worker is not running, so stats might be stale. Let's refresh them.
                downloadStatusViewModel.updateDownloadStats()
            }
        }
    }


    private fun showCreatePlaylistDialog() {
        val context = requireContext()
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = "Enter playlist name..."
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("New Playlist")
            .setView(editText)
            .setPositiveButton("Create") { dialog, _ ->
                val playlistName = editText.text.toString()
                if (playlistName.isNotBlank()) {
                    playlistsViewModel.createNewPlaylist(playlistName)
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
