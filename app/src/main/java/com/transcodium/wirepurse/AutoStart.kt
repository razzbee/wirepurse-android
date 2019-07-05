package com.transcodium.wirepurse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.transcodium.wirepurse.classes.NotificationCore

class AutoStart : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        //start app notifications job
        NotificationCore().startNotificationsJob(context)

    }
}