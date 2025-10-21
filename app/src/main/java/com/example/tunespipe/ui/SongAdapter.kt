package com.example.tunespipe.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner // <-- ADD THIS
import androidx.lifecycle.lifecycleScope            // <-- ADD THIS
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tunespipe.MusicPlayerSingleton // <-- ADD THIS
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn             // <-- ADD THIS
import kotlinx.coroutines.flow.onEach               // <-- ADD THIS

class SongRecyclerView(
    private var songs: List<Song>,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerView.SongViewHolder>() {

    private var loadingSong: Song? = null

    // --- START OF NEW CODE: Observe the global state when adapter is attached ---
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // Find the lifecycle of the fragment/activity this RecyclerView lives in.
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycleScope?.let { scope ->
            // Start observing the global nowPlaying state.
            MusicPlayerSingleton.nowPlaying
                .onEach { nowPlayingSong ->
                    // When the global state changes, update the adapter's internal state
                    // and redraw the entire list to show/hide the correct spinner.
                    loadingSong = nowPlayingSong
                    notifyDataSetChanged()
                }
                .launchIn(scope) // The observation will automatically cancel when the lifecycle is destroyed.
        }
    }
    // --- END OF NEW CODE ---

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

    // --- START OF CHANGE: Remove the now-obsolete setPlaying function ---
    // The setPlaying function is no longer needed.
    // fun setPlaying(song: Song) { ... }
    // --- END OF CHANGE ---

    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }
}
