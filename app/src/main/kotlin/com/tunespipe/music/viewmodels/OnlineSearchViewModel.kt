package com.tunespipe.music.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tunespipe.innertube.YouTube
import com.tunespipe.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.tunespipe.innertube.models.filterExplicit
import com.tunespipe.music.constants.HideExplicitKey
import com.tunespipe.music.models.ItemsPage
import com.tunespipe.music.utils.dataStore
import com.tunespipe.music.utils.get
import com.tunespipe.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow(FILTER_SONG)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (viewStateMap[filter.value] == null) {
                    YouTube
                        .search(query, filter)
                        .onSuccess { result ->
                            viewStateMap[filter.value] =
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(
                                            context.dataStore.get(
                                                HideExplicitKey,
                                                false
                                            )
                                        ),
                                    result.continuation,
                                )
                        }.onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value.value
        viewModelScope.launch {
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
