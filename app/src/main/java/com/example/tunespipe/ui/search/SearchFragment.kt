package com.example.tunespipe.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentSearchBinding
import com.example.tunespipe.searchITunes
import com.example.tunespipe.ui.SongRecyclerView
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setupSearchView()
        // --- START OF CHANGE: Setup RecyclerView upfront ---
        setupRecyclerView()
        // --- END OF CHANGE ---
        return binding.root
    }

    // --- START OF CHANGE: New setup method ---
    private fun setupRecyclerView() {
        // The adapter is created once and the RecyclerView is configured.
        val songAdapter = SongRecyclerView { clickedSong ->
            Log.d("SearchFragment", "User clicked: ${clickedSong.trackName}")

            val songActionsDialog = SongActionsDialogFragment.newInstance(clickedSong)
            songActionsDialog.show(childFragmentManager, "SongActionsDialog")
        }
        binding.searchResultsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
        }
    }
    // --- END OF CHANGE ---

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            @UnstableApi
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                if (!queryString.isNullOrBlank()) {
                    lifecycleScope.launch {
                        binding.searchView.clearFocus()
                        // --- START OF CHANGE: Get adapter and submit list ---
                        val songs = searchITunes(queryString)
                        (binding.searchResultsRecycler.adapter as? SongRecyclerView)?.submitList(songs)
                        // --- END OF CHANGE ---
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

    // The old displaySearchResults function is no longer needed
}
