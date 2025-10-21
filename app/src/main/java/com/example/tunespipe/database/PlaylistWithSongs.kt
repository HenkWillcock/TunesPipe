package com.example.tunespipe.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.tunespipe.Song

/**
 * This class is used by Room to hold the result of a query that
 * joins a Playlist with its related list of Songs.
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: Playlist,

    // --- START OF CHANGE ---
    @Relation(
        parentColumn = "id",            // Primary key of the Playlist entity
        entityColumn = "trackId",       // Primary key of the Song entity
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId", // Column in PlaylistSongCrossRef that points to Playlist
            entityColumn = "songId"     // Column in PlaylistSongCrossRef that points to Song
        )
    )
    // --- END OF CHANGE ---
    val songs: List<Song>
)
