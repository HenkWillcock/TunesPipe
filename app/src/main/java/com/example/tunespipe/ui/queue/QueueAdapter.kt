package com.example.tunespipe.ui.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tunespipe.AutoplayStrategy
import com.example.tunespipe.R
import com.example.tunespipe.databinding.ItemQueueAutoplayBinding
import com.example.tunespipe.databinding.ItemQueueSongBinding
import com.example.tunespipe.databinding.ItemQueueNowPlayingBinding
import java.lang.IllegalArgumentException

class QueueAdapter(
    private var items: List<QueueItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NOW_PLAYING = 0
        private const val TYPE_QUEUED_SONG = 1
        private const val TYPE_AUTOPLAY = 2
    }

    inner class NowPlayingViewHolder(val binding: ItemQueueNowPlayingBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class QueuedSongViewHolder(val binding: ItemQueueSongBinding) :
        RecyclerView.ViewHolder(binding.root)

    // --- ViewHolder for the "Autoplay Strategy" item ---
    inner class AutoplayViewHolder(val binding: ItemQueueAutoplayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is QueueItem.NowPlaying -> TYPE_NOW_PLAYING
            is QueueItem.QueuedSong -> TYPE_QUEUED_SONG
            is QueueItem.Autoplay -> TYPE_AUTOPLAY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_NOW_PLAYING -> {
                val binding = ItemQueueNowPlayingBinding.inflate(inflater, parent, false)
                NowPlayingViewHolder(binding)
            }
            TYPE_QUEUED_SONG -> {
                val binding = ItemQueueSongBinding.inflate(inflater, parent, false)
                QueuedSongViewHolder(binding)
            }
            TYPE_AUTOPLAY -> {
                val binding = ItemQueueAutoplayBinding.inflate(inflater, parent, false)
                AutoplayViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val currentItem = items[position]) {
            is QueueItem.NowPlaying -> {
                val nowPlayingHolder = holder as NowPlayingViewHolder
                val song = currentItem.song
                nowPlayingHolder.binding.songTitleText.text = song.trackName
                nowPlayingHolder.binding.artistNameText.text = song.artistName
                Glide.with(holder.itemView.context)
                    .load(song.artworkUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(nowPlayingHolder.binding.artworkImage)
            }
            is QueueItem.QueuedSong -> {
                val queuedHolder = holder as QueuedSongViewHolder
                val song = currentItem.song
                queuedHolder.binding.songInfoText.text = "${song.trackName}\n${song.artistName}"
            }
            is QueueItem.Autoplay -> {
                val autoplayHolder = holder as AutoplayViewHolder
                val strategy = currentItem.strategy
                when (strategy) {
                    is AutoplayStrategy.RepeatOne -> {
                        autoplayHolder.binding.strategyText.text = "Repeat One"
                        autoplayHolder.binding.strategyText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one_24, 0, 0, 0)
                    }
                    is AutoplayStrategy.ShufflePlaylist -> {
                        val playlistName = strategy.playlistWithSongs.playlist.name
                        autoplayHolder.binding.strategyText.text = "Shuffle Playlist: $playlistName"
                        autoplayHolder.binding.strategyText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle_24, 0, 0, 0)
                    }
                }
            }
        }
    }

    fun updateItems(newItems: List<QueueItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
