package com.example.tunespipe.ui.playlists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.tunespipe.DownloadManager
import com.example.tunespipe.DownloadWorker
import com.example.tunespipe.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()

    // LiveData for UI
    private val _downloadedCount = MutableLiveData(0)
    val downloadedCount: LiveData<Int> = _downloadedCount

    private val _pendingCount = MutableLiveData(0)
    val pendingCount: LiveData<Int> = _pendingCount

    private val _currentlyDownloading = MutableLiveData<String?>(null)
    val currentlyDownloading: LiveData<String?> = _currentlyDownloading

    // Observe the worker's status
    val downloadWorkerInfo: LiveData<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkLiveData("PlaylistDownloadWorker")

    fun updateDownloadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val allSongsInPlaylists = playlistDao.getAllPlaylistsWithSongs()
                .flatMap { it.songs }
                .distinctBy { it.trackId }

            var downloaded = 0
            for (song in allSongsInPlaylists) {
                if (DownloadManager.getSongFile(getApplication(), song).exists()) {
                    downloaded++
                }
            }

            val pending = allSongsInPlaylists.size - downloaded

            // Post values to LiveData on the main thread
            _downloadedCount.postValue(downloaded)
            _pendingCount.postValue(pending)
        }
    }
}
