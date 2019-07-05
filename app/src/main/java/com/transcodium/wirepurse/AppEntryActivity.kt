package com.transcodium.wirepurse

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.transcodium.wirepurse.classes.Crypt
import com.transcodium.wirepurse.classes.Account
import com.transcodium.wirepurse.classes.NotificationCore
import com.transcodium.wirepurse.classes.getData
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class AppEntryActivity : AppCompatActivity() {


    private val mActivity by lazy{ this }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var checkTwoFA = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        Fabric.with(this, Crashlytics())



        //init app
        val isOk = initApp()


        //if initialization is okay, proceed with app
        if(isOk) {

            if (BuildConfig.DEBUG) {
                com.facebook.stetho.Stetho.initializeWithDefaults(this)
            }

            val introCompleted = sharedPref().getBoolean("intro_completed", false)

            //if if intro is not completed, then next activity class is intro
            if (!introCompleted) {
                startClassActivity(AppIntroActivity::class.java,true)
            }

            //if user is already logged in
            else if(isLoggedIn()) {

                startInAppAuth()

                checkTwoFA = false

            } else {

                //start login
                startClassActivity(LoginActivity::class.java,true)
            }

        }//end if everything is ok


    }//end fun

    override fun onBackPressed() {
        confirmExitDialog(this)
    }

    fun initApp(): Boolean{

        //check if app key exists, if not create a new one
        val createAppKey = try{
            Crypt().createAppKey(this)
        } catch (e: Exception){

            null
        }

        if(createAppKey == null || createAppKey.isError()){

             dialog {
                setTitle(R.string.initialization_error)
                setMessage(R.string.initialization_error_message)
                setPositiveButton(R.string.ok){_,_ -> mActivity.finish()}
             }

            return false
        }//end if error

        /**
         * calling getDeviceID will create it when it doesnt exist,
         * this creates a unique uuid onetime for identifying user device
         */
        getDeviceId(this)

        /**
         * start App Notification service
         */
         NotificationCore().startNotificationsJob(mActivity)

        return true
    }//end fun


    //onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode != INAPP_AUTH_REQUEST_CODE){
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        val status = data?.getStatus()

        val account = Account(this)

        if(status == null || status.isError()){
            account.doLogout(status)
            return
        }


        account.handlePostLogin(
             activity = this,
             checkTwoFa = checkTwoFA
        )
    }//end on activity result



}//end class
