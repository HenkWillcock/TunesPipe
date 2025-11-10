package com.tunespipe.music.models

import com.tunespipe.innertube.models.YTItem
import com.tunespipe.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
