package com.example.tunespipe

// --- START OF FIX: Import Parcelable and Parcelize ---
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
// --- END OF FIX ---


// --- START OF FIX: Add @Parcelize annotation ---
@Parcelize
sealed interface AutoplayStrategy : Parcelable {
// --- END OF FIX ---

    // --- START OF FIX: Add @Parcelize annotation ---
    @Parcelize
    data object RepeatOne : AutoplayStrategy
    // --- END OF FIX ---

    // --- START OF FIX: Add @Parcelize annotation ---
    @Parcelize
    data class ShufflePlaylist(val playlist: List<Song>) : AutoplayStrategy
    // --- END OF FIX ---
}
