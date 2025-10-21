package com.example.tunespipe.ui

import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SongRecyclerView(
    private val onSongClicked: (Song) -> Unit
) : ListAdapter<Song, SongRecyclerView.SongViewHolder>(SongsComparator()) {

    private var playingSong: Song? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycleScope?.let { scope ->
            MusicPlayerSingleton.nowPlaying
                .onEach { nowPlayingSong ->
                    val oldPlayingSong = playingSong
                    playingSong = nowPlayingSong
                    // Instead of redrawing everything, we find the items that changed and update only them.
                    val oldIndex = currentList.indexOf(oldPlayingSong)
                    if (oldIndex != -1) notifyItemChanged(oldIndex)
                    val newIndex = currentList.indexOf(nowPlayingSong)
                    if (newIndex != -1) notifyItemChanged(newIndex)
                }
                .launchIn(scope)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song, song == playingSong, onSongClicked)
    }

    class SongViewHolder(private val binding: ItemSongResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, isPlaying: Boolean, onSongClicked: (Song) -> Unit) {
            binding.trackName.text = song.trackName
            binding.artistName.text = song.artistName

            Glide.with(itemView.context)
                .load(song.artworkUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.artworkImage)

            binding.loadingSpinner.visibility = if (isPlaying) View.VISIBLE else View.GONE

            // --- START OF REVERTED LOGIC ---
            if (isPlaying) {
                // Apply "now playing" styles with a hardcoded color
                binding.trackName.setTypeface(null, Typeface.BOLD)
                binding.trackName.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.teal_200)
                )
            } else {
                // Apply default styles
                binding.trackName.setTypeface(null, Typeface.NORMAL)
                // Using android.R.color.white as the default. Change if you have a different default.
                binding.trackName.setTextColor(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )
            }
            // --- END OF REVERTED LOGIC ---

            itemView.setOnClickListener {
                onSongClicked(song)
            }
        }
    }

    class SongsComparator : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            // ID is the unique identifier
            return oldItem.trackId == newItem.trackId
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            // Check if the contents are the same. A data class `equals` check is perfect here.
            return oldItem == newItem
        }
    }
}
