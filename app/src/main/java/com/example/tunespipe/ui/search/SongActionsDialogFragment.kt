package com.example.tunespipe.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tunespipe.R
import com.example.tunespipe.Song
import com.example.tunespipe.database.AppDatabase
import com.example.tunespipe.database.PlaylistSongCrossRef
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SongActionsDialogFragment : BottomSheetDialogFragment() {

    private lateinit var song: Song

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the Song object that was passed to this dialog
        song = arguments?.getParcelable(ARG_SONG) ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_actions, container, false)
        val rootLayout = view as LinearLayout // The root of your layout is a LinearLayout

        // --- START OF UI SETUP ---
        // Populate the views that are already in your XML
        view.findViewById<TextView>(R.id.song_title_text).text = song.trackName
        view.findViewById<TextView>(R.id.artist_name_text).text = song.artistName
        view.findViewById<MaterialButton>(R.id.play_now_button).setOnClickListener {
            // Your existing MusicPlayerSingleton logic would go here
            Toast.makeText(context, "Playing ${song.trackName}", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        val dao = AppDatabase.getDatabase(requireContext()).playlistDao()

        // --- START OF DYNAMIC BUTTON CREATION ---
        lifecycleScope.launch {
            val playlists = dao.getAllPlaylists().first()

            if (playlists.isNotEmpty()) {
                // Add a divider and a title before listing the playlists
                rootLayout.addView(MaterialDivider(requireContext()).apply {
                    val margin = (16 * resources.displayMetrics.density).toInt()
                    (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, margin, 0, margin)
                })
                rootLayout.addView(TextView(context).apply {
                    text = "Add to playlist"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                })

                // For each playlist, create and add a button
                playlists.forEach { playlist ->
                    val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        text = "Add to '${playlist.name}'"
                        setOnClickListener { addSongToPlaylist(playlist.id, song) }
                    }
                    rootLayout.addView(button)
                }
            }
        }
        // --- END OF DYNAMIC BUTTON CREATION ---

        return view
    }

    private fun addSongToPlaylist(playlistId: Long, song: Song) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).playlistDao()
            // 1. First, save the Song object to the 'songs' table.
            dao.insertSong(song)
            // 2. Then, create the link between the playlist and the song.
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, song.trackId))

            Toast.makeText(context, "'${song.trackName}' added to playlist!", Toast.LENGTH_SHORT).show()
            dismiss() // Close the dialog
        }
    }

    companion object {
        private const val ARG_SONG = "song"

        // A clean way to create an instance of this dialog and pass the Song data to it
        fun newInstance(song: Song): SongActionsDialogFragment {
            return SongActionsDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SONG, song)
                }
            }
        }
    }
}
