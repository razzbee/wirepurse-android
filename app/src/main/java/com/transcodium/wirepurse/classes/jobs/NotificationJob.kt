package com.transcodium.wirepurse.classes.jobs


import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.transcodium.wirepurse.IO
import com.transcodium.wirepurse.classes.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class NotificationJob: JobService(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    var coroutineJob: Job? = null

    val mContext by lazy { this }

    /**
     * doWork
     */
    override fun onStartJob(job: JobParameters): Boolean {

        val ctx = this

        if(!Account(ctx).isLoggedIn()){
            return false
        }

        IO.launch {
            //check for notifications
            NotificationCore().fetchAndProcessNotifications(mContext,true)
        }

        return false
    }//end fun

    /**
     * onStopJob
     */
    override fun onStopJob(job: JobParameters?): Boolean {

        coroutineJob?.cancel()

        return false
    }


} //end class
