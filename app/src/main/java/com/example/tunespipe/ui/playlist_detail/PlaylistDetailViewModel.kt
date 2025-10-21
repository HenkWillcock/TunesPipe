package com.example.tunespipe.ui.playlist_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tunespipe.database.PlaylistDao
import com.example.tunespipe.database.PlaylistWithSongs
import kotlinx.coroutines.launch

/**
 * ViewModel for the PlaylistDetailFragment.
 */
class PlaylistDetailViewModel(
    private val playlistDao: PlaylistDao, // Make dao private val
    playlistId: Long
) : ViewModel() {
    // --- START OF FIX: LiveData is now nullable ---
    val playlistWithSongs: LiveData<PlaylistWithSongs?> = playlistDao.getPlaylistWithSongs(playlistId).asLiveData()
    // --- END OF FIX ---

    fun deletePlaylist() {
        // We can get the playlist object from the live data
        val playlistToDelete = playlistWithSongs.value?.playlist
        if (playlistToDelete != null) {
            viewModelScope.launch {
                playlistDao.delete(playlistToDelete)
            }
        }
    }
}

/**
 * Factory for creating a PlaylistDetailViewModel with a constructor that takes a playlistId.
 */
class PlaylistDetailViewModelFactory(
    private val playlistDao: PlaylistDao,
    private val playlistId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistDetailViewModel(playlistDao, playlistId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
