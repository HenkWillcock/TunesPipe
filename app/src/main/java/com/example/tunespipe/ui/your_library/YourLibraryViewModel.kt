package com.example.tunespipe.ui.your_library

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tunespipe.database.Playlist
import com.example.tunespipe.database.PlaylistDao
import kotlinx.coroutines.launch

class YourLibraryViewModel(private val playlistDao: PlaylistDao) : ViewModel() {

    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylists().asLiveData()

    /**
     * Launch a new coroutine to insert a playlist in a non-blocking way
     */
    fun createNewPlaylist(playlistName: String) {
        viewModelScope.launch {
            playlistDao.insert(Playlist(name = playlistName))
        }
    }
}
