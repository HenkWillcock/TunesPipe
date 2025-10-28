package com.example.tunespipe.ui.queue

import com.example.tunespipe.AutoplayStrategy
import com.example.tunespipe.Song

/**
 * A sealed interface to represent the different types of items
 * that can appear in the queue's RecyclerView.
 */
sealed interface QueueItem {
    data class NowPlaying(val song: Song) : QueueItem
    data class QueuedSong(val song: Song) : QueueItem
    data class Autoplay(val strategy: AutoplayStrategy) : QueueItem
}
