package com.transcodium.wirepurse.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject


class LiveDataTransport : ViewModel() {

    val data: MutableLiveData<JSONObject> by lazy {
        MutableLiveData<JSONObject>()
    }

}