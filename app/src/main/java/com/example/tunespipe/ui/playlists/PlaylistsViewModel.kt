package com.example.tunespipe.ui.playlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.tunespipe.Song
import com.example.tunespipe.database.Playlist
import com.example.tunespipe.database.PlaylistDao
import com.example.tunespipe.database.PlaylistSongCrossRef
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val playlistDao: PlaylistDao) : ViewModel() {

    val allPlaylists: LiveData<List<Playlist>> = playlistDao.getAllPlaylists().asLiveData()

    fun createNewPlaylist(playlistName: String) {
        viewModelScope.launch {
            playlistDao.insert(Playlist(name = playlistName))
        }
    }

    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch {
            playlistDao.insertSong(song)
            playlistDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = song.trackId))
        }
    }

    suspend fun isSongInPlaylist(songId: String, playlistId: Long): Boolean {
        return playlistDao.doesSongExistInPlaylist(playlistId, songId) > 0
    }

    fun removeSongFromPlaylist(songId: String, playlistId: Long) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
