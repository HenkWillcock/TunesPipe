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
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentSearchBinding
import com.example.tunespipe.searchITunes
import com.example.tunespipe.ui.SongAdapter
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
        return binding.root
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            @UnstableApi
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                if (!queryString.isNullOrBlank()) {
                    lifecycleScope.launch {
                        binding.searchView.clearFocus()  // Stops SearchView handling event itself
                        displaySearchResults(searchITunes(queryString))
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // TODO search real time.
                return false // Let the SearchView handle its default behavior
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @UnstableApi
    private fun displaySearchResults(songs: List<Song>) {
        binding.searchResultsRecycler.apply {
            layoutManager = LinearLayoutManager(context)  // Arranges items in a 1D list.
            adapter = SongAdapter(songs) { clickedSong: Song ->
                Log.d("SearchFragment", "User clicked: ${clickedSong.trackName}")

                lifecycleScope.launch {
                    Log.d("SearchFragment", "11111111")
                    MusicPlayerSingleton.playSongFromSearch(
                        requireContext(),
                        "${clickedSong.artistName} - ${clickedSong.trackName}",
                    )
                }
            }
        }
    }
}
