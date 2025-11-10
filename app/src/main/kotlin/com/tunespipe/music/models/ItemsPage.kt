package com.tunespipe.music.models

import com.tunespipe.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
