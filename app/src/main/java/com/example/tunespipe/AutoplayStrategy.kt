package com.example.tunespipe

/**
 * Defines the behavior for what should happen when a song finishes playing.
 * Using a sealed class allows states to hold their own specific data.
 */
sealed class AutoplayStrategy {
    /**
     * Repeats the same song indefinitely. This state has no extra data.
     */
    data object RepeatOne : AutoplayStrategy()

    /**
     * Picks another random song from the given playlist. This state holds the playlist.
     * @param playlist The list of songs to shuffle through.
     */
    data class ShufflePlaylist(val playlist: List<Song>) : AutoplayStrategy()
}
