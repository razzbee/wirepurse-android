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
#  created_at 09/08/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import com.tapadoo.alerter.Alerter
import com.transcodium.wirepurse.R
import com.transcodium.wirepurse.UI
import com.transcodium.wirepurse.vibrate
import kotlinx.coroutines.launch

class AppAlert(val activity: Activity) {

    private val duration = 5000L

   private fun alertObj(): Alerter {

       hide()

       return Alerter.create(activity)
                .enableVibration(false)
                .enableIconPulse(true)
                .setDismissable(true)
                .enableSwipeToDismiss()
                .enableInfiniteDuration(true)
    }

    /**
     * processAlert
     */
    private  fun processAlert(message: Any,autoClose : Boolean = false, bgColorRes: Int) = UI.launch {
        activity.runOnUiThread {

            val alertObj = alertObj()

            val messageStr = if (message is Int) {
                activity.getString(message)
            } else {
                message.toString()
            }

            alertObj.setBackgroundColorRes(bgColorRes)
                    .setText(messageStr)
                    .setIcon(R.drawable.ic_error_outline_pink_24dp)

            if (autoClose) {
                alertObj.enableInfiniteDuration(false)
                        .setDuration(duration)
            }

            activity.vibrate()
            alertObj.show()
        }
    } //end fun

    /**
     * error
     */
    fun error(message: Any,autoClose : Boolean = false) = UI.launch{
        processAlert(message,autoClose,R.color.colorAccent)
    }


    /**
     * success
     */
    fun success(message: Any,autoClose : Boolean = true) = UI.launch {
        processAlert(message,autoClose,R.color.green)
    }//end if

    /**
     * info
     */
    fun info(message: Any,autoClose : Boolean = true) = UI.launch {
        processAlert(message,autoClose,R.color.colorPrimary)
    }//end if



    /**
     * showStatus
     */
    fun showStatus(status: Status){

        activity.runOnUiThread {

            if (Alerter.isShowing) {
                Alerter.hide()
            }

            if (status.isError()) {
                error(status.getMessage(activity))
            } else if (status.isSuccess()) {
                success(status.getMessage(activity))
            } else if(status.isNeutral()){

            }
        }

    }//end fun


    /**
     * hide
     */
    fun hide(){
        activity.runOnUiThread {
            if (Alerter.isShowing) {
                Alerter.hide()
            }
        }
    }


}//end class