package com.example.tunespipe.ui

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// --- START OF FIX: Import the ViewModel ---
import com.example.tunespipe.MusicPlayerViewModel
// --- END OF FIX ---
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SongRecyclerViewAdapter(
    private var songs: List<Song>,
    // --- START OF FIX: Add ViewModel to constructor ---
    private val playerViewModel: MusicPlayerViewModel,
    // --- END OF FIX ---
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerViewAdapter.SongViewHolder>() {

    private var playingSong: Song? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycleScope?.let { scope ->
            // --- START OF FIX: Observe the ViewModel's nowPlaying flow ---
            playerViewModel.nowPlaying
                .onEach { nowPlayingSong ->
                    playingSong = nowPlayingSong
                    // Using notifyDataSetChanged is inefficient but simple for now.
                    // It's okay for this project size.
                    notifyDataSetChanged()
                }
                .launchIn(scope)
            // --- END OF FIX ---
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

        // The rest of this file is correct. The logic for highlighting the
        // playing song based on the 'playingSong' variable remains the same.
        if (playingSong?.trackId == song.trackId) {
            holder.binding.loadingSpinner.visibility = if (playingSong?.previewUrl.isNullOrEmpty()) View.VISIBLE else View.GONE
            setTextSelected(holder.binding.trackName, holder.itemView.context)
            setTextSelected(holder.binding.artistName, holder.itemView.context)
        } else {
            holder.binding.loadingSpinner.visibility = View.GONE
            setTextNotSelected(holder.binding.trackName, holder.itemView.context)
            setTextNotSelected(holder.binding.artistName, holder.itemView.context)
        }

        holder.itemView.setOnClickListener {
            onSongClicked(song)
        }
    }

    private fun setTextSelected(text: TextView, context: Context) {
        text.setTypeface(null, Typeface.BOLD)
        text.setTextColor(ContextCompat.getColor(context, R.color.teal_200))
    }

    private fun setTextNotSelected(text: TextView, context: Context) {
        text.setTypeface(null, Typeface.NORMAL)
        text.setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    override fun getItemCount() = songs.size

    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }
}
