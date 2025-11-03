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
import com.example.tunespipe.DownloadManager
import com.example.tunespipe.MusicPlayerViewModel
import com.example.tunespipe.NetworkUtils
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SongRecyclerViewAdapter(
    private var songs: List<Song>,
    private val playerViewModel: MusicPlayerViewModel,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerViewAdapter.SongViewHolder>() {

    private var playingSong: Song? = null
    // --- START OF FIX: Add a variable to track loading state ---
    private var isLoading: Boolean = false
    // --- END OF FIX ---

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val lifecycleOwner = recyclerView.findViewTreeLifecycleOwner()
        lifecycleOwner?.lifecycleScope?.let { scope ->
            playerViewModel.nowPlaying
                .onEach { nowPlayingSong ->
                    playingSong = nowPlayingSong
                    notifyDataSetChanged()
                }
                .launchIn(scope)

            // --- START OF FIX: Observe the isLoading state from the ViewModel ---
            playerViewModel.isLoading
                .onEach { loading ->
                    isLoading = loading
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
        val context = holder.itemView.context
        holder.binding.trackName.text = song.trackName
        holder.binding.artistName.text = song.artistName

        Glide.with(holder.itemView.context)
            .load(song.artworkUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.binding.artworkImage)

        val isCurrentlyPlayingOrLoadingSong = playingSong?.trackId == song.trackId

        // --- START OF FIX: Update spinner visibility logic ---
        // Show the spinner ONLY if this is the currently "playing" song AND we are in a loading state.
        if (isCurrentlyPlayingOrLoadingSong && isLoading) {
            holder.binding.loadingSpinner.visibility = View.VISIBLE
        } else {
            holder.binding.loadingSpinner.visibility = View.GONE
        }
        // --- END OF FIX ---

        if (isCurrentlyPlayingOrLoadingSong) {
            setTextSelected(holder.binding.trackName, holder.itemView.context)
            setTextSelected(holder.binding.artistName, holder.itemView.context)
        } else {
            setTextNotSelected(holder.binding.trackName, holder.itemView.context)
            setTextNotSelected(holder.binding.artistName, holder.itemView.context)
        }

        holder.itemView.setOnClickListener {
            onSongClicked(song)
        }

        holder.binding.notDownloadedText.visibility = View.GONE
        holder.itemView.alpha = 1.0f
        holder.itemView.isClickable = true

        // Now, apply offline state if necessary
        if (!NetworkUtils.isOnline(context)) {
            val songFile = DownloadManager.getSongFile(context, song)
            if (!songFile.exists()) {
                // We are offline AND the song is not downloaded.
                // Grey out the item, show the text, and disable clicks.
                holder.binding.notDownloadedText.visibility = View.VISIBLE
                holder.itemView.alpha = 0.5f
                holder.itemView.isClickable = false
            }
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
