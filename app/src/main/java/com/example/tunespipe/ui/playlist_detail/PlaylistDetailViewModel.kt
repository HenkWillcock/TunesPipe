package com.example.tunespipe.ui.playlist_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.tunespipe.database.PlaylistDao
import com.example.tunespipe.database.PlaylistWithSongs

/**
 * ViewModel for the PlaylistDetailFragment.
 */
class PlaylistDetailViewModel(playlistDao: PlaylistDao, playlistId: Long) : ViewModel() {
    // Hold a LiveData object of the playlist with its songs.
    // This will automatically update when the data changes in the database.
    val playlistWithSongs: LiveData<PlaylistWithSongs> = playlistDao.getPlaylistWithSongs(playlistId).asLiveData()
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
