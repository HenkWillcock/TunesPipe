package com.example.tunespipe.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
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

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    /**
     * Fetches a single playlist and all of its associated songs.
     * The @Transaction annotation is crucial here. It ensures that this complex
     * query (which fetches from multiple tables) is executed atomically.
     */
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    /**
     * Checks if a specific song is already in a specific playlist.
     * Returns 1 if the relationship exists, 0 otherwise.
     */
    @Query("SELECT COUNT(*) FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun doesSongExistInPlaylist(playlistId: Long, songId: String): Int

    /**
     * Removes a song from a playlist by deleting the entry in the cross-reference table.
     */
    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
}
