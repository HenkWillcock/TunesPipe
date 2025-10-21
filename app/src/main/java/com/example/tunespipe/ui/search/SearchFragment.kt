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
import com.example.tunespipe.ui.SongRecyclerViewAdapter
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    // --- START OF CHANGE ---
    // Remove the unnecessary class property for the adapter.
    // var songAdapter: SongRecyclerViewAdapter? = null
    // --- END OF CHANGE ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setupSearchView()
        return binding.root
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
        // --- START OF CHANGE ---
        // Declare the adapter as a local variable. It does not need to be a class property.
        val songAdapter = SongRecyclerViewAdapter(songs) { clickedSong ->
            // --- END OF CHANGE ---
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
