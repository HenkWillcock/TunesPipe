package com.example.tunespipe.ui.queue

import com.example.tunespipe.AutoplayStrategy
import com.example.tunespipe.Song

sealed class QueueItem {
    data class NowPlaying(val song: Song) : QueueItem()
    data class QueuedSong(val song: Song) : QueueItem()
    data class Autoplay(val strategy: AutoplayStrategy) : QueueItem()
    data class Header(val title: String) : QueueItem()
}
