package com.transcodium.wirepurse.classes

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProviders
import com.firebase.jobdispatcher.JobService


import com.transcodium.wirepurse.*
import com.transcodium.wirepurse.classes.jobs.NotificationJob
import com.transcodium.wirepurse.db.AppDB
import com.transcodium.wirepurse.db.entities.Notifications
import com.transcodium.wirepurse.view_models.NotificationViewModel
import kotlinx.android.synthetic.main.notification_menu_action_layout.*
import kotlinx.android.synthetic.main.notification_menu_action_layout.view.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NotificationCore {

    val walletCore = WalletCore()

    fun startNotificationsJob(mContext: Context){
        IO.async {
            startPeriodicJob(
                    mContext = mContext,
                    tag = "serverNotification",
                    clazz = NotificationJob::class,
                    triggerInterval = Pair(30, 60),
                    lifeTime = "forever"
            )
        }
    }//end fun


    /**
     * setSeen
     */
    suspend fun setSeen(
            context: Context,
            remoteIds: MutableList<String>
    ): Status {

        val userInfo = context.getUserInfo()
                ?: return errorStatus(R.string.auth_required)

        val userId = userInfo.getString("user_id")

        val notificationDao = AppDB.getInstance(context).notificationDao()

        notificationDao.setSeenByRemoteIds(userId,remoteIds.toList())

        return TnsApi(context).post(
                requestPath = "/notifications/set-seen",
                params =  listOf(Pair("ids",remoteIds.joinToString(","))),
                hasAuth = true
        )
    }

    /**
     * processNotificationCount
     */
     fun processNotificationCount(
            activity: AppCompatActivity,
            view: View
     ) {

        val notifIconParent = view.notifMenuBtn

        val notificationCountTextView = view.notificationCountTextView

        //process notifications
        val anim = android.view.animation.AnimationUtils
                        .loadAnimation(activity, R.anim.wiggle_effect)


        val viewProvider = try{
            ViewModelProviders.of(activity)
                    .get(NotificationViewModel::class.java)
        } catch (e: java.lang.Exception){
            e.printStackTrace()

            return
        }

        var stopAnim = false


        viewProvider.getUnseenNotifcationCount().observeForever { totalUnseen->


           if(notificationCountTextView == null){
                return@observeForever
           }


            notificationCountTextView.text = totalUnseen.toString()


            if (totalUnseen == 0) {

                notificationCountTextView.visibility = View.GONE

                stopAnim = true

                return@observeForever

            } else {
                stopAnim = false
            }

            notificationCountTextView.visibility = View.VISIBLE

            UI.async {
                while (true) {

                    if (stopAnim) {
                        break
                    }

                    notifIconParent?.startAnimation(anim)

                    delay(8000)
                }
            } //end Async
        } //end observer
    }//end fun

    /**
     * process Notif Date
     */


    /**
     * fetch and process Notifications
     */
    suspend fun fetchAndProcessNotifications(
            mContext: Context,
            showSystemNotification: Boolean = true
    ): Status {

        val userInfo = mContext.getUserInfo()

        if(userInfo == null){
            Log.e("UserInfoErr","Not Logged in")
            return errorStatus(R.string.auth_required)
        }

        val userId = userInfo.getString("user_id")

        val notifStatus = TnsApi(mContext)
                .get(
                        requestPath = "/notifications",
                        hasAuth = true
                )

        if(notifStatus.isError()){
            Log.e("FETCH_NOTIF_ERR",notifStatus.getMessage(mContext))
            return notifStatus
        }

        val dataArray = try{
            notifStatus.getData<JSONArray>()
        } catch (e: Exception){

            //println("---> Status : $notifStatus")
            e.printStackTrace()
            return errorStatus()
        }

        if(dataArray == null || dataArray.size() == 0){
            return notifStatus
        }


        val totalNotifs = dataArray.size()


      //  println("------->>>>>>>>${notifStatus.toJsonString()}")

        val notifGroup = "${this::class.java.name}.notifications"

        val notifs = mutableListOf<Notification>()

        val database = AppDB.getInstance(mContext)

        val notifDao = database.notificationDao()


        var hasRefreshedUserAssets = false


        var  notifPendingIntent: PendingIntent? = null

        if(showSystemNotification){

            val notifIntent = Intent(mContext,NotificationsActivity::class.java).apply{
                flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            }

            notifPendingIntent = PendingIntent
                   .getActivity(mContext, 0, notifIntent, 0)
        }

        //println("------DATA-->> $dataArray")

        for(index in dataArray.indexRange()){

            val dataObj = dataArray[index] as JSONObject

            val remoteId = dataObj.getString("id")

            //if item exists already exit
            val itemExists = notifDao.exists(userId,remoteId)

            //if it exists, skip it
            if(itemExists == 1){
                continue
            }

            val title = dataObj.getString("title")
            val message = dataObj.getString("message")
            val type = dataObj.getString("type")
            val createdAt = dataObj.getLong("created_at")

            val typeStr = type.replace("_"," ").capitalize()


            //refresh user assets
            //hhere we will pull user assets only if there is a
            //notification
            //if(!hasRefreshedUserAssets){

                //if(type.contains("transaction")){
                    IO.async {  walletCore.networkFetchUserAssets(mContext) }
              //  }

                //set to true
               // hasRefreshedUserAssets = true
            //} //end if

            //remove the remote id to prevent duplicate in sqlite db
            dataObj.remove("id")
            dataObj.remove("created_at")

            val notifDbData = Notifications(
                    userId = userId,
                    data = dataObj.toString(),
                    seen = 0,
                    remoteId = remoteId,
                    createdAt = createdAt
            )

            //insert notification
            notifDao.insert(notifDbData)


            //if showSystemNotification, then process notifications
            if(showSystemNotification) {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //add channel
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val newChannel = NotificationChannel(type, typeStr, importance).apply {
                        setShowBadge(true)
                        shouldShowLights()
                        shouldVibrate()
                    }

                    val notificationManager = mContext.getSystemService(JobService.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(newChannel)
                }

                val notif = NotificationCompat.Builder(mContext, type)
                        .setSmallIcon(R.drawable.logo_only_blue)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                        .setContentIntent(notifPendingIntent!!)
                        .setGroup(notifGroup)
                        .setAutoCancel(false)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .build()

                notifs.add(notif)
            }//end if show notif
        } //end loop


        if(showSystemNotification) {
            NotificationManagerCompat.from(mContext).apply {
                for ((index, notifObj) in notifs.withIndex()) {
                    notify(
                            System.currentTimeMillis().toInt(),
                            notifObj
                    )
                }
            } //end show notif
        }//end if


        return successStatus()
    } //end fun

}//end fun