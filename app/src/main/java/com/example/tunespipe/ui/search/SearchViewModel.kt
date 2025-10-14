package com.example.tunespipe.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = (
            "TODO: Search box, show iTunes API results.\n" +
            "Clicking one shoves the song title plus 'Lyric Video' to play with NewPipeExtractor."
        )
    }
    val text: LiveData<String> = _text
}
