package com.example.tunespipe.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.NetworkUtils
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentSearchBinding
import com.example.tunespipe.searchITunes
import com.example.tunespipe.ui.SongRecyclerViewAdapter
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Get a reference to the shared ViewModel from the activity
    private val playerViewModel: MusicPlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setupSearchView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (NetworkUtils.isOnline(requireContext())) {
            // We are online, show the normal UI
            binding.searchView.isVisible = true
            binding.searchResultsRecycler.isVisible = true
            binding.offlineMessageText.isVisible = false
            setupSearchView()
        } else {
            // We are offline, show the offline message
            binding.searchView.isVisible = false
            binding.searchResultsRecycler.isVisible = false
            binding.offlineMessageText.isVisible = true
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            @UnstableApi
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                if (!queryString.isNullOrBlank()) {
                    lifecycleScope.launch {
                        binding.searchView.clearFocus()
                        displaySearchResults(searchITunes(queryString))
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @UnstableApi
    private fun displaySearchResults(songs: List<Song>) {
        // Pass the playerViewModel into the adapter's constructor
        val songAdapter = SongRecyclerViewAdapter(songs, playerViewModel) { clickedSong ->
            Log.d("SearchFragment", "User clicked: ${clickedSong.trackName}")

            val songActionsDialog = SongActionsDialogFragment.newInstance(clickedSong)
            songActionsDialog.show(childFragmentManager, "SongActionsDialog")
        }
        binding.searchResultsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
        }
    }
}
