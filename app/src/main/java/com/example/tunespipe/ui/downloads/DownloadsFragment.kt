package com.example.tunespipe.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.work.WorkInfo
import com.example.tunespipe.DownloadWorker
import com.example.tunespipe.databinding.FragmentDownloadsBinding
import com.example.tunespipe.ui.playlists.DownloadStatusViewModel

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private val downloadStatusViewModel: DownloadStatusViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDownloadStatusObservers()
    }

    // This is the same logic from the old PlaylistsFragment
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
                // Worker is not running, refresh stats in case we missed an update
                downloadStatusViewModel.updateDownloadStats()
            }
        }
    }

    // Refresh stats when the fragment is resumed
    override fun onResume() {
        super.onResume()
        downloadStatusViewModel.updateDownloadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
