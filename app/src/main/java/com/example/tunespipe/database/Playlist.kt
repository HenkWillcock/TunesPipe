package com.example.tunespipe.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tunespipe.Song
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String
)


@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    // --- START OF NEW CODE ---

    /**
     * Inserts a song into the 'songs' table.
     * `OnConflictStrategy.IGNORE` means if a song with the same trackId already exists,
     * Room will simply ignore the insert operation and not throw an error.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: Song)

    /**
     * Inserts a link between a playlist and a song into the cross-reference table.
     * This is how we add a song to a playlist.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    // --- END OF NEW CODE ---
}
