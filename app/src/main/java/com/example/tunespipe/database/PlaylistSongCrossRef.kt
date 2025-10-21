package com.example.tunespipe.database

import androidx.room.Entity

/**
 * Represents the link between a Playlist and a Song.
 * Each row in this table means "this song is in this playlist".
 * The primary key is a combination of both IDs to ensure a song
 * cannot be added to the same playlist more than once.
 */
@Entity(primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String // Must match the type of Song's PrimaryKey (trackId)
)
