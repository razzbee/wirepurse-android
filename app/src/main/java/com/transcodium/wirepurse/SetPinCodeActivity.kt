package com.transcodium.wirepurse

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.transcodium.wirepurse.classes.FingerprintCore
import kotlinx.android.synthetic.main.activity_set_pin_code.*
import org.jetbrains.anko.toast

class SetPinCodeActivity : RootActivity() {

    var onFinishedActivity: Class<*> = HomeActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin_code)

        //lets get onFinishedActivity
        val intentData = intent.extras

        val hasOnfinishedClazz = intentData?.containsKey("on_finished_activity") ?: false

        //activity to open when finished
        if(hasOnfinishedClazz){

            val onFinishedClazzStr = intentData
                    ?.getString("on_finished_activity",null)

            if(onFinishedClazzStr != null){
               onFinishedActivity = Class.forName(onFinishedClazzStr)
            }
        }//end if

        setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimaryDark))

        val fingerprint = FingerprintCore(this)

        if(!fingerprint.hasHardware() &&
           Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ){
            //disable fingerprint support
            enableFingerprintSupport.isChecked = false
            enableFingerprintSupport.visibility = View.GONE
        }

        savePinBtn.setOnClickListener {

            val pinCode = pinCodeInput.text!!.toString()

            pinCodeInputLayout.error = ""

            if(pinCode.length.compareTo(4) != 0){
                vibrate()
                pinCodeInputLayout.error = getString(R.string.pincode_lenght_error)
                return@setOnClickListener
            }


            val confirmPinCode = confirmPinCodeInput.text!!.toString()


            confirmPinCodeInputLayout.error = ""

            if(confirmPinCode.compareTo(pinCode) != 0){
                vibrate()
                confirmPinCodeInputLayout.error = getString(R.string.pincodes_dont_match)
                return@setOnClickListener
            }//end if


            var isFingerSupportEnabled = enableFingerprintSupport.isChecked

            //enable fingerprint
            //if the fingerprint support is enabled, lets check if user
            //has enrolled fingerprint
            if(
                    isFingerSupportEnabled &&
                     fingerprint.hasHardware() &&
                    !fingerprint.isEnabled()
            ){

                val setting = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
                    Settings.ACTION_SETTINGS
                } else {
                    Settings.ACTION_FINGERPRINT_ENROLL
                }

                AlertDialog.Builder(this)
                        .setMessage(R.string.enable_fingerprint_setting)
                        .setNegativeButton(R.string.cancel){d,_-> d.dismiss()}
                        .setPositiveButton(R.string.ok){d,_->
                            d.dismiss()
                            startActivity(Intent(setting))
                        }.show()
                return@setOnClickListener
            }//end if


            //now lets save the fingerprint
            val pinCodeHash = pinCode.toSha256()

            @SuppressWarnings
            if(isFingerSupportEnabled){
                val createFMStatus = fingerprint.create()

                if(createFMStatus.isError()){

                    isFingerSupportEnabled = false

                    toast(createFMStatus.message())
                }
            }

            //save  data
            sharedPref().edit {
                putString("pin_code",pinCodeHash)
                putBoolean("fingerprint_enabled",isFingerSupportEnabled)
            }

            toast(R.string.pincode_saved)

            //nagivate to home
            startClassActivity(onFinishedActivity,true)
        }//end onClick listener


    }//end fun

}//end class
