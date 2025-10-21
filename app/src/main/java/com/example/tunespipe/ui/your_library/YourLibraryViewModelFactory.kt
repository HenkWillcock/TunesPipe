package com.example.tunespipe.ui.your_library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tunespipe.database.PlaylistDao

class YourLibraryViewModelFactory(private val playlistDao: PlaylistDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YourLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YourLibraryViewModel(playlistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
