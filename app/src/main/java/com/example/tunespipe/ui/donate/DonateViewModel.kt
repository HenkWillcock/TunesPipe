package com.example.tunespipe.ui.donate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DonateViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = (
            "This app will always be free, but donating shows your appreciation and allows me to continue development.\n" +
            "NZers can transfer directly to my NZ account: 38-9006-0259129-08 which avoids transaction fees.\n" +
            "Anyone else, please donate with (TODO: Stripe or something)"
        )
    }
    val text: LiveData<String> = _text
}
