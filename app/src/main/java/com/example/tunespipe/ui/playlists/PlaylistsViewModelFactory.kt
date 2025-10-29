package com.example.tunespipe.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tunespipe.database.PlaylistDao

class PlaylistsViewModelFactory(private val playlistDao: PlaylistDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistsViewModel(playlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
