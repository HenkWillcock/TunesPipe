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
import com.example.tunespipe.MusicPlayerSingleton
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.databinding.ItemSongResultBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SongRecyclerViewAdapter(
    private var songs: List<Song>,
    private val onSongClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongRecyclerViewAdapter.SongViewHolder>() {

    private var playingSong: Song? = null

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
