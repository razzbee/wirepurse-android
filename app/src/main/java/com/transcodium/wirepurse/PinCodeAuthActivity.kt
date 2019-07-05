package com.transcodium.wirepurse


import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.os.CancellationSignal
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_pin_code_auth.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast

class PinCodeAuthActivity : RootActivity() {

    private val mActivity by lazy { this }

    private var fingerPrintRetryAttempts = 5

    private var pinCodeRetryAttempt = 5

    private var fingerprintCancellationSignal: CancellationSignal? = null

    private var isFingerPrintPaused = false

    private val hasFingerprint   by lazy {
        sharedPref().getBoolean("fingerprint_enabled",false)
    }

    //fingerprint UI delay Job
    private var fpUICancelDelayJob: Job? = null


    //lets get pincode
    private val savedPinCodeHash by lazy { sharedPref().getString("pin_code", null) }

    private var closeOnSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        //get intent data
        val intentData = intent.extras

        //if not set, open the activity
        if (savedPinCodeHash == null) {

            //if it was manually disabled, no need to set it
            val pincodeManuallyDisbaled = sharedPref().getBoolean("pincode_manually_disabled",false)

            //force new pincode
            val forceNewPinCode = intentData?.getBoolean("force_new_pincode",false) ?: false

            if(pincodeManuallyDisbaled && !forceNewPinCode){
                handleAuthSuccess(showToast = false)
                return
            }


            //if here, then prompt user to set it
            startClassActivity(SetPinCodeActivity::class.java, true)
            return
        }//end if

        setContentView(R.layout.activity_pin_code_auth)

        setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        handleFingerprint()

        //switch from fingerprint to pincode auth
        usePinCode.setOnClickListener {
            showPincodeUI(false)
        }

        if(hasFingerprint) {
            //switch to fingerprint
            useFingerprint.setOnClickListener { handleFingerprint() }
        }else{
            useFingerprint.hide()
        }//end if


        /**
         * cancelBtn
         */
        cancelBtn.setOnClickListener { handleAuthFailure() }

        /**
         * pincode continue btn
         */
        pincodeContinueBtn.setOnClickListener { handlePinCodeAuth() }

    }//end fun

    override fun onBackPressed() {

        toast(R.string.in_app_auth_cancelled_by_user)

        val data = Intent()
        data.setStatus(errorStatus(getString(R.string.in_app_auth_failed)))

        setResult(Activity.RESULT_CANCELED,data)

        this.finish()
    }

    /**
     * cancel fingerprint if paused
     */
    override fun onPause() {
        super.onPause()
        fingerprintCancellationSignal?.cancel()
        isFingerPrintPaused = true
    }

    override fun onResume() {
        super.onResume()

        if(isFingerPrintPaused && hasFingerprint){
            handleFingerprint()
        }
    }//end on resume


    /**
     * processPinCodeAuth
     */
    fun handlePinCodeAuth(){

       val pinCode = pinCodeInput.text!!

       pinCodeInputLayout.error = ""

       if(pinCode.length < 4){
           vibrate()
           pinCodeInputLayout.error = getString(R.string.pincode_lenght_error)
           return
       }

       //lets hash the pin and compare
       val userHashedPin = pinCode.toString().toSha256()

       //validate
       if(savedPinCodeHash!!.compareTo(userHashedPin!!) == 0){
           handleAuthSuccess()
           return
       }//end if


        /**
         * Auth Failed
         */
        vibrate()

        pinCodeRetryAttempt -= 1

        pinCodeInputLayout.error = getString(R.string.invalid_pincode,pinCodeRetryAttempt.toString())

        //after 5 times of failure, fail permanently
        if(pinCodeRetryAttempt == 0){
            handleAuthFailure()
        }//end if

    }//end fun


    /**
     * HandleFingerPrint
     */
    fun handleFingerprint(){

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return
        }

        //if fingerprint was not enabled, remove the fingerprint
        if(!hasFingerprint){
            showPincodeUI(true)
            return
        }

        val fingerprintCore = FingerprintCore(this)

        //here, lets check if user has deleted fingerprint enrollment data
        //to avoid crashing
        if(!fingerprintCore.isEnabled()){
            longToast(R.string.fingerprint_records_not_found)

            //disable it
            sharedPref().edit{
                putBoolean("fingerprint_enabled",false)
            }

            //remove use fingerprint btn
            useFingerprint.hide()

            return
        }//end if

        //cancel any fpUICancelDelayJob job
        fpUICancelDelayJob?.cancel()

        //show fingerpring UI
        showFingerprintUI()


        val authStatus = fingerprintCore.authenticate(
                onAuthError = {errCode,errMsg -> onFingerprintError(errCode,errMsg) },
                onAuthFailed = { onFingerprintFailed() },
                onAuthSuccess = {  handleAuthSuccess() }
        )

        if(authStatus.isError()){
            showPincodeUI(true)
            return
        }

        fingerprintCancellationSignal = authStatus.getData()


        //also we have noticed that the current activity with fingerprint
        //gets closed after some time of inactivity
        //so after 30 seconds of no touch, we will disable fingerprint to use
        //pin
        fpUICancelDelayJob = UI.launch {

            delay(30000L)

            longToast(R.string.idle_fingerprint_ui_closed)

            fingerprintCancellationSignal?.cancel()

            showPincodeUI()
        }//end avoid wasting resources when idle

    }//end

    /**
     * onFingerprintError
     */
    fun onFingerprintError(errCode: Int, errMsg: String?){

        Log.e("FINGERPRINT_ERROR","Error Code: $errCode : Message: $errMsg")

        val err = if(errMsg != null){
            errMsg
        }else{
            getString(R.string.fingerprint_failed_msg)
        }

        longToast(err)

        showPincodeUI(true)
    }//end fun


    /**
     * handle authFailed
     */
    fun onFingerprintFailed(){

        //decrement
        fingerPrintRetryAttempts -= 1

        toast(getString(R.string.retry_msg,fingerPrintRetryAttempts.toString()))


        if(fingerPrintRetryAttempts == 0){

            toast(R.string.fingerprint_auth_failed)

            showPincodeUI(true)
            return
        }

    }//end


    /**
     * authSuccess
     */
    private fun handleAuthSuccess(showToast: Boolean? = true){

        fpUICancelDelayJob?.cancel()

        if(showToast!!) {
            longToast(R.string.in_app_auth_success)
        }

        val data = Intent()
        data.setStatus(successStatus())
        setResult(Activity.RESULT_OK,data)

        finish()
    }//end fun


    /**
     * handleAuthFailure
     */
    private fun handleAuthFailure(){

        fpUICancelDelayJob?.cancel()

        val appLockExpiry = System.currentTimeMillis() + (APP_LOCK_EXPIRY * 6000)

        sharedPref().edit{
            putLong("app_locked_for",appLockExpiry)
            putBoolean("force_login",true)
        }

        val data = Intent()
        data.setStatus(errorStatus(R.string.in_app_auth_failed))
        setResult(Activity.RESULT_OK,data)

        val dialogMsg = mActivity.getString(R.string.in_app_auth_failed)

        //show logout dialog
        dialog {
            setCancelable(false)
            setMessage(dialogMsg)
            setPositiveButton(R.string.ok){d,w->
                d.dismiss()
                Account(mActivity).doLogout()
                mActivity.finish()
            }
        }

    }//end

    /**
     * showFingerprintUI
     */
    fun showFingerprintUI(){
        pincodeViewParent.visibility = View.GONE
        fingerprintViewParent.visibility = View.VISIBLE
    }

    /**
     * showPincodeUI
     */
    fun showPincodeUI(hideUseFingerprint: Boolean = false){

        //if fingerprint is showing, cancel it
        fingerprintCancellationSignal?.cancel()

        fpUICancelDelayJob?.cancel()

        pinCodeInput.requestFocus()

        pincodeViewParent.show()
        fingerprintViewParent.hide()

        if(hasFingerprint){
            useFingerprint.show()
        }

    }//end fun


}//end class


