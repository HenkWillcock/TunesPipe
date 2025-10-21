package com.example.tunespipe.ui.your_library

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tunespipe.Song
import com.example.tunespipe.database.Playlist
import com.example.tunespipe.database.PlaylistDao
import com.example.tunespipe.database.PlaylistSongCrossRef
import kotlinx.coroutines.launch

class YourLibraryViewModel(private val playlistDao: PlaylistDao) : ViewModel() {

    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylists().asLiveData()

    fun createNewPlaylist(playlistName: String) {
        viewModelScope.launch {
            playlistDao.insert(Playlist(name = playlistName))
        }
    }

    // --- START OF NEW CODE ---
    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch {
            // First, ensure the song exists in the 'songs' table so it can be referenced.
            playlistDao.insertSong(song)
            // Then, create the link in the cross-reference table.
            playlistDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = song.trackId))
        }
    }
    // --- END OF NEW CODE ---
}
