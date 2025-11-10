package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
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
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
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
