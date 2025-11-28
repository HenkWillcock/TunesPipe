package com.tunespipe.innertube.pages

import com.tunespipe.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
