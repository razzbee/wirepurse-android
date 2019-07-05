package com.transcodium.wirepurse

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.edit
import com.transcodium.wirepurse.classes.Account
import com.transcodium.wirepurse.classes.AppAlert
import com.transcodium.wirepurse.classes.getData
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.activity_setting.view.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.longToast

class SettingActivity : AppCompatActivity() {

    val userInfo by lazy {
        getUserInfo()
    }

    var hasError = false

    val appAlert by lazy { AppAlert(this) }

    private val DISABLE_INAPPAUTH_PINCODE_INTENT_CODE = 712

    private val ENABLE_2FA_INTENT_CODE = 797

    private val DISABLE_2FA_INTENT_CODE = 700


    private val mActivity by lazy { this }

    val intentData = Bundle().apply {
        putString("on_finished_activity", SettingActivity::class.java.name)
    }

    /**
     * since the inAppAuth requires verification
     * from activity result, we will need to
     * get a pending state of what to do after
     */
    var pendingDisablePinCode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        UI.launch {

            processProfileInfo()

            processInAppAuth()

            process2FA()
        }
    }


    //on Back Pressed
    override fun onBackPressed() {

        if(isTaskRoot) {
            startClassActivity(activityClass = HomeActivity::class.java)
            finish()
        } else {
            super.onBackPressed()
        }

        super.onBackPressed()
    }

    /**
     * updateUserFullName
     */
    private fun processProfileInfo(){

        userFullNameValue.text = (userInfo!!.optString("user_full_name",""))

        accountEmail.accountEmail.text = (userInfo!!.optString("user_email",""))


        //if the card is clicked, it should show edit
        accountInfoCard.setOnClickListener { v->
           startClassActivity(AccountUpdateActivity::class.java)
        }//end on Click

    }//end fun


    /**
     * processInAppAuth
     */
    private fun processInAppAuth(){

        val isPinCodeEnabled = isPinCodeEnabled()

        inAppAuthStatus.isChecked = isPinCodeEnabled

        val intent = Intent(mActivity,PinCodeAuthActivity::class.java)

        inAppAuthStatus.setOnCheckedChangeListener { buttonView, isChecked ->

            pendingDisablePinCode = false

            if(!isChecked){

                if(!isPinCodeEnabled){
                    return@setOnCheckedChangeListener
                }

                //validate user first
                pendingDisablePinCode = true

                //start auth
                startActivityForResult(intent, DISABLE_INAPPAUTH_PINCODE_INTENT_CODE)


            } else { // at this point its checked

                sharedPref().edit {
                    remove("pincode_manually_disabled")
                }


                startClassActivity(
                        activityClass = SetPinCodeActivity::class.java,
                        data = intentData
                )
            }//end if

        }//end change listener
    }//end fun


    /**
     * process2FA
     */
    suspend fun process2FA(){

        val has2FAStatus = Account(this).has2FA(false)

        if(has2FAStatus.isError()){
            appAlert.showStatus(has2FAStatus)
            return
        }

        val has2FA = has2FAStatus.getData<Boolean>()!!

        twoFAStatus.isChecked = has2FA

        twoFAStatus.setOnCheckedChangeListener { buttonView, isChecked ->

             if(isChecked){

                startClassActivity(
                        activityClass = Enable2faActivity::class.java,
                        data = intentData
                )
            } else {

                 startClassActivity(
                         activityClass = Disable2faActivity::class.java,
                         data = intentData
                 )

            } //end if
        } //end event


    }//end end fun


    /**
     * onActivity Result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        //if pincode auth is successful
        if (requestCode == DISABLE_INAPPAUTH_PINCODE_INTENT_CODE) {


            if(resultCode == Activity.RESULT_OK) {

                sharedPref().edit {
                    remove("pin_code")
                    remove("fingerprint_enabled")
                    putBoolean("pincode_manually_disabled",true)
                }

                longToast(R.string.inapp_auth_disabled)

                val isPinCodeEnabled = isPinCodeEnabled()

                inAppAuthStatus.isChecked = isPinCodeEnabled

            } else {

                //failed

                longToast(R.string.in_app_auth_failed)

                //reopen activity class
                startClassActivity(
                        activityClass = this::class.java,
                        clearActivityStack = true
                )

                //finish the old settings activity
                this.finish()
                return
            }

            super.onActivityResult(requestCode,resultCode,data)
        }//end if disable pincode



    }//end fun



}//end class
