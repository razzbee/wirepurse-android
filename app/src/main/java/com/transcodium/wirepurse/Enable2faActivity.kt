package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_enable_2fa.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.dip
import org.jetbrains.anko.toast
import org.json.JSONObject

class Enable2faActivity : AppCompatActivity() {

    private val mActivity by lazy { this }

    private val appAlert by lazy { AppAlert(this) }

    private val appLoader by lazy { Progress(this) }

    private var onFinishedActivity: Class<*> = HomeActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_enable_2fa)

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

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setTitle(R.string.enable_twofa)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        this.setStatusBarColor(getColorRes(R.color.colorPrimaryDark))

        UI.launch {

            progressBar.show()

            //lets check if user has 2fa
            val has2fAStatus = Account(mActivity).has2FA(doRemoteCheck = true)

            if(has2fAStatus.isError()){
                progressBar.hide()
                appAlert.showStatus(has2fAStatus)
                return@launch
            }

            disable2faBtn.setOnClickListener {

                val intentBundle = Bundle().apply {
                    putString("on_finished_activity", SettingActivity::class.java.name)
                }

                startClassActivity(
                     activityClass = Disable2faActivity::class.java,
                     data = intentBundle
                )

                return@setOnClickListener
            }

            val has2fa = has2fAStatus.getData<Boolean>()!!


            if(has2fa){
                progressBar.hide()
                toolbar.setBackgroundResource(R.color.colorPrimaryDark)
                twoFAStatus.visibility = View.VISIBLE
                return@launch
            }


            //lets get new 2fa data
            //get user info
            val create2faStatus = TnsApi(mActivity).get(
                    requestPath = "/account/create-2fa",
                    hasAuth = true
            )

            progressBar.hide()

            if(create2faStatus.isError()){
                appAlert.showStatus(create2faStatus)
                return@launch
            }


            val twoFaData = create2faStatus.getData<JSONObject?>()

            if(twoFaData == null){
                mActivity.fireLog("Create2FAError","Remote Data null")
                errorStatus(R.string.unexpected_error)
                return@launch
            }

            twoFaDataView.visibility = View.VISIBLE

            val secretKey = twoFaData.getString("secret_key")

            val label = getString(R.string.app_name)

            val qrCodeUri = "otpauth://totp/$label?secret=$secretKey"

            //generate qr code
            generateQRCode(qrCodeUri, dip(280),twofaQRImageView)

            secretKeyInput.setText(secretKey)


            secretKeyInput.setOnClickListener { v  ->
                copyToClipboard(secretKeyInput.text.toString())
                toast(R.string.secret_key_copied)
            }

            /**
             * request verification Code
             */
            requestCodeBtn.setOnClickListener {
                UI.launch {

                    VerificationCode(mActivity).sendCode(
                            activityGroup = "2fa_setting",
                            hasAuth = true
                    )
                }
            }//end resend code

            //if form is submitted
            verifyAndSave.setOnClickListener {


                var hasError = false

                verificationCodeInputLayout.error = ""
                twoFACodeInputLayout.error = ""

                val verifCode = verificationCodeInput.text.toString()

                if(verifCode.isEmpty()){
                    verificationCodeInputLayout.error = getString(R.string.verification_code_required)
                    hasError = true
                }

                val twoFACode = twoFACodeInput.text.toString()

                if(!twoFACode.isInt()){
                    twoFACodeInputLayout.error = getString(R.string.twofa_code_must_be_numeric)
                    hasError = true
                }

                if(hasError){ return@setOnClickListener }


                UI.launch {

                    appLoader.show()

                    val params = listOf<Pair<String,Any>>(
                            Pair("verification_code",verifCode),
                            Pair("twofa",twoFACode)
                    )

                    val save2FAStatus =  TnsApi(mActivity).post(
                            requestPath = "/account/verify-and-save-2fa",
                            params = params,
                            hasAuth = true
                    )

                    appLoader.hide()

                    appAlert.showStatus(save2FAStatus)

                    //close if success
                    if(save2FAStatus.isError()) {
                        return@launch
                    }


                    Account(mActivity).updateLocalUserInfo("has_2fa",true)

                    delay(2000)

                    //nagivate back
                    startClassActivity(onFinishedActivity,true)

                }//end llaunch

            }

        } //end launch

    } //end oncreate


    //go back
    override fun onBackPressed() {

        startClassActivity(
                SettingActivity::class.java,
                clearActivityStack = true
        )

        this.finish()

        super.onBackPressed()
    }


} //end class
