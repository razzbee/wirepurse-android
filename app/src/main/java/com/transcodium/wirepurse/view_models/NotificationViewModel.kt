package com.transcodium.wirepurse.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

import com.transcodium.wirepurse.db.AppDB
import com.transcodium.wirepurse.getUserInfo

class NotificationViewModel(application: Application) : AndroidViewModel(application){

    //user info
    private val userInfo = application.getUserInfo()!!

    private val userId = userInfo.getString("user_id")

    private val appDB: AppDB = AppDB.getInstance(application)

    private val notifDao = appDB.notificationDao()

    private val unseenNotificationCount: LiveData<Int> = notifDao
                                      .getTotalUnseenCountLive(userId)


    private val fetchLatestLive = notifDao.fetchAllLive(userId)



    /**
     * getUnseenNotifcationCount
     */
    fun getUnseenNotifcationCount() = unseenNotificationCount

    fun fetchLatest() = fetchLatestLive

}