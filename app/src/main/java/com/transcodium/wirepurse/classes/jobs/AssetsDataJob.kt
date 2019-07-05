/**
# Copyright 2018 - Transcodium Ltd.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the  Apache License v2.0 which accompanies this distribution.
#
#  The Apache License v2.0 is available at
#  http://www.opensource.org/licenses/apache2.0.php
#
#  You are required to redistribute this code under the same licenses.
#
#  Project TNSMoney
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 25/08/2018
 **/

package com.transcodium.wirepurse.classes.jobs


import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.transcodium.wirepurse.classes.Account
import com.transcodium.wirepurse.classes.WalletCore
import kotlinx.coroutines.*

import kotlin.coroutines.CoroutineContext

class AssetsDataJob : JobService(), CoroutineScope {

    val walletCore = WalletCore()

    override val coroutineContext: CoroutineContext
                    get() = Dispatchers.IO

    var coroutineJob: Job? = null

    /**
     * doWork
     */
    override fun onStartJob(job: JobParameters): Boolean {

        val ctx = this

        coroutineJob = launch {

            if(!Account(ctx).isLoggedIn()){
                stopSelf()
                return@launch
            }

                walletCore.networkFetchUserAssets(ctx)
            //Log.i("UPDATING USER STATS","True -----")
        }

        //wait for job to finish
        coroutineJob?.onJoin

        return false
    }//end fun

    /**
     * onStopJob
     */
    override fun onStopJob(job: JobParameters?): Boolean {

        coroutineJob?.cancel()

        return false
    }

}//end class