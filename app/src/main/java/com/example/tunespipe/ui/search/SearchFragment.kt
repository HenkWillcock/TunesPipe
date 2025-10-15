package com.example.tunespipe.ui.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.tunespipe.MusicPlayerService
import com.example.tunespipe.MusicPlayerSingleton
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
            @UnstableApi
            override fun onQueryTextSubmit(queryString: String?): Boolean {
                if (!queryString.isNullOrBlank()) {
                    Log.d("SearchFragment", "Search submitted: $queryString")

                    // Prevent the SearchView from handling the event by itself
                    binding.searchView.clearFocus()

                    // 2. Launch the coroutine to search and play the song
                    //    This logic remains the same, but now it operates on a player
                    //    that is managed by a long-running service.
                    lifecycleScope.launch {
                        try {
                            MusicPlayerSingleton.playSongFromSearch(queryString)

                            // Create an Intent to start the MusicPlayerService
                            val serviceIntent = Intent(
                                requireContext(),
                                MusicPlayerService::class.java,
                            )
                            requireContext().startService(serviceIntent)

                        } catch (e: Exception) {
                            Log.e("SearchFragment", "Error playing song from search", e)
                        }
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
