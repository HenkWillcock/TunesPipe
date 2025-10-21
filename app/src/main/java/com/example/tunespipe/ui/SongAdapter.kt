package com.example.tunespipe.ui

import android.graphics.Typeface // <-- ADD THIS IMPORT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // <-- ADD THIS IMPORT
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SongRecyclerView(
    private var songs: List<Song>,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerView.SongViewHolder>() {

    private var playingSong: Song? = null // Renamed for clarity from loadingSong

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycleScope?.let { scope ->
            MusicPlayerSingleton.nowPlaying
                .onEach { nowPlayingSong ->
                    playingSong = nowPlayingSong
                    notifyDataSetChanged()
                }
                .launchIn(scope)
        }
    }

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

        if (playingSong == song) {
            holder.binding.loadingSpinner.visibility = View.VISIBLE
            holder.binding.trackName.setTypeface(null, Typeface.BOLD)
            holder.binding.trackName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.teal_700)
            )
        } else {
            holder.binding.loadingSpinner.visibility = View.GONE
            holder.binding.trackName.setTypeface(null, Typeface.NORMAL)
            holder.binding.trackName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
        }

        holder.itemView.setOnClickListener {
            onSongClicked(song)
        }
    }

    override fun getItemCount() = songs.size

    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }
}
