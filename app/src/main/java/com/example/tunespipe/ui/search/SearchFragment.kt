package com.example.tunespipe.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import com.example.tunespipe.databinding.FragmentSearchBinding

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
            // This method is called when the user hits "enter" or the search button.
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    Log.d("SearchFragment", "Search submitted: $query")
                    // Prevent the SearchView from handling the event by itself
                    binding.searchView.clearFocus()
                    // TODO: Here is where you will trigger the actual search
                    // For example: searchViewModel.searchForSong(query)
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
