package com.example.tunespipe

import android.os.Parcelable
import com.example.tunespipe.database.PlaylistWithSongs
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
sealed interface AutoplayStrategy : Parcelable {

    @Parcelize
    data object RepeatOne : AutoplayStrategy

    @Parcelize
    data class ShufflePlaylist(
        val playlistWithSongs: @RawValue PlaylistWithSongs
    ) : AutoplayStrategy
}
