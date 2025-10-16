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
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.FragmentSearchBinding
import com.example.tunespipe.searchITunes
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
                    // TODO make this the iTunes search with a UI.
                    // https://itunes.apple.com/search?term=jack+johnson
                    //  Only plays the song when you click an option.
                    lifecycleScope.launch {
                        binding.searchView.clearFocus()  // Stops SearchView handling event itself

                        val songs = searchITunes(queryString)

                        Log.d("TunesPipe", "Found songs: ${songs}")

//                        MusicPlayerSingleton.playSongFromSearch(
//                            requireContext(),
//                            queryString,
//                        )
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
}
