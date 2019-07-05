package com.transcodium.wirepurse

import android.os.Bundle
import android.os.PersistableBundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

open class DialogActivity : AppCompatActivity(), CoroutineScope {

    var  job: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val winSize = getWindowSize()

        val winWith = winSize.widthPixels * 0.95

        val winHeight = winSize.heightPixels * 0.80

        window.setLayout(
                winWith.toInt(),
                winHeight.toInt()
        )

        job = Job()
    }
}