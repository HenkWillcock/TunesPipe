package com.example.tunespipe.ui.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tunespipe.R
import com.example.tunespipe.database.Playlist

// --- START OF CHANGE: Add a click listener function to the constructor ---
class PlaylistAdapter(private val onItemClicked: (Playlist) -> Unit) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(PlaylistsComparator()) {
// --- END OF CHANGE ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_item, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val current = getItem(position)
        // --- START OF CHANGE: Set the click listener on the item's view ---
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
        // --- END OF CHANGE ---
        holder.bind(current.name)
    }

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistNameView: TextView = itemView.findViewById(R.id.playlist_name_textview)

        fun bind(text: String?) {
            playlistNameView.text = text
        }
    }

    class PlaylistsComparator : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}
