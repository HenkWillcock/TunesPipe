package com.tunespipe.innertube.pages

import com.tunespipe.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
