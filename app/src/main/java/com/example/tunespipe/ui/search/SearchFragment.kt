package com.example.tunespipe.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tunespipe.MusicPlayer
import com.example.tunespipe.databinding.FragmentSearchBinding
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

    // TODO
    // Add iTunes search with a UI.
    // https://itunes.apple.com/search?term=jack+johnson
    // Once I can do that, everything else is just building an interface for the iTunes API.
    // Playlists, Radio, Jams, etc.

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // This method is called when the user hits "enter" or the search button.
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                if (!queryString.isNullOrBlank()) {
                    Log.d("SearchFragment", "Search submitted: $queryString")

                    // Prevent the SearchView from handling the event by itself
                    binding.searchView.clearFocus()

                    // TODO need to refactor so there's like a constant thread for playing the music.
                    // Hitting playSongFromSearch just changes the song,
                    // doesn't start a whole new process.
                    lifecycleScope.launch {
                        MusicPlayer.playSongFromSearch(queryString)
                    }
                }
                return true
            }

            // This method is called for every character change in the search box.
            override fun onQueryTextChange(newText: String?): Boolean {
                // We don't need to do anything here for now.
                // Useful for showing search suggestions in real-time.
                return false // Let the SearchView handle its default behavior
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
