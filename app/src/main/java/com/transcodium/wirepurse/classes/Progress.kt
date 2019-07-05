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
import com.transcodium.wirepurse.vibrate

class Progress(val activity: Activity) {

    /**
     * show
     */
    fun show(
            title: Any? = R.string.loading,
            text: Any? = R.string.default_loading_text,
            bgColor: Int = R.color.colorPrimaryDark,
            blockUI: Boolean = true,
            dismissable: Boolean = false
    ) {

        activity.runOnUiThread {

            if (Alerter.isShowing) {
                hide()
            }

            val titleStr: String = if (title is Int) {
                activity.getString(title)
            } else {
                title.toString()
            }

            val textStr = if (text is Int) {
                activity.getString(text)
            } else {
                text.toString()
            }

            val alertObj = Alerter.create(activity)
                    .enableVibration(false)
                    .enableIconPulse(false)
                    .enableInfiniteDuration(true)
                    .enableProgress(true)
                    .setBackgroundColorRes(bgColor)
                    .setDismissable(dismissable)


            if (blockUI) {
                alertObj.disableOutsideTouch()
            }

            if (dismissable) {
                alertObj.enableSwipeToDismiss()
            }

            if (!titleStr.isEmpty()) {
                alertObj.setTitle(titleStr)
            }

            if (!textStr.isEmpty()) {
                alertObj.setText(textStr)
            }

            activity.vibrate(listOf(0L, 5L))

            alertObj.show()
        }
    }//end fun

    fun hide() {
        activity.runOnUiThread {  Alerter.hide() }
    }
}