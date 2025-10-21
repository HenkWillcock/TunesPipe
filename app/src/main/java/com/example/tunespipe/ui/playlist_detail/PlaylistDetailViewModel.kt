package com.example.tunespipe.ui.playlist_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tunespipe.database.PlaylistDao
import com.example.tunespipe.database.PlaylistWithSongs
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistDao: PlaylistDao, // Make dao private val
    playlistId: Long
) : ViewModel() {
    val playlistWithSongs: LiveData<PlaylistWithSongs?> = playlistDao.getPlaylistWithSongs(playlistId).asLiveData()

    fun deletePlaylist() {
        val playlistToDelete = playlistWithSongs.value?.playlist
        if (playlistToDelete != null) {
            viewModelScope.launch {
                playlistDao.delete(playlistToDelete)
            }
        }
    }
}

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
