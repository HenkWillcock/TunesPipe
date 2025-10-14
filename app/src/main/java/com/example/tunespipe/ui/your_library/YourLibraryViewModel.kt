package com.example.tunespipe.ui.your_library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class YourLibraryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is your library Fragment"
    }
    val text: LiveData<String> = _text
}