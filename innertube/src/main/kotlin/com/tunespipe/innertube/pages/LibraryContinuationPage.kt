package com.tunespipe.innertube.pages

import com.tunespipe.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
