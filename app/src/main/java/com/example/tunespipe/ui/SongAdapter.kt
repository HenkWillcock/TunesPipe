package com.example.tunespipe.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding

// --- START OF CHANGE: Change `songs` from val to var ---
class SongRecyclerView(
    private var songs: List<Song>,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerView.SongViewHolder>() {
// --- END OF CHANGE ---

    private var loadingSong: Song? = null

    class SongViewHolder(val binding: ItemSongResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.binding.trackName.text = song.trackName
        holder.binding.artistName.text = song.artistName

        Glide.with(holder.itemView.context)
            .load(song.artworkUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.binding.artworkImage)

        // Show the spinner ONLY if this song is the one that's loading.
        holder.binding.loadingSpinner.visibility =
            if (loadingSong == song) View.VISIBLE
            else View.GONE

        holder.itemView.setOnClickListener {
            onSongClicked(song)
        }
    }

    override fun getItemCount() = songs.size

    fun setPlaying(song: Song) {
        loadingSong = song
        notifyDataSetChanged() // Redraw the entire list to show/hide spinners.
    }

    // --- START OF NEW CODE: Add the missing updateSongs function ---
    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged() // Tell the adapter that the data has changed
    }
    // --- END OF NEW CODE ---
}
