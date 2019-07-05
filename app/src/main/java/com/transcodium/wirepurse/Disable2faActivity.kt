package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_disable_2fa.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.twofa_view_full.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Disable2faActivity : AppCompatActivity() {

    private val mActivity by lazy { this }

    private val appAlert by lazy { AppAlert(this) }

    private val appLoader by lazy { Progress(this) }

    private var onFinishedActivity: Class<*> = HomeActivity::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disable_2fa)


        //lets get onFinishedActivity
        val intentData = intent.extras

        val hasOnfinishedClazz = intentData?.containsKey("on_finished_activity") ?: false

        //activity to open when finished
        if (hasOnfinishedClazz) {

            val onFinishedClazzStr = intentData
                    ?.getString("on_finished_activity", null)

            if (onFinishedClazzStr != null) {
                onFinishedActivity = Class.forName(onFinishedClazzStr)
            }
        }//end if

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setTitle(R.string.disable_2fa)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        this.setStatusBarColor(getColorRes(R.color.colorPrimaryDark))


        UI.launch {

            progressBar.show()

            //lets check if user has 2fa
            val has2fAStatus = Account(mActivity).has2FA(doRemoteCheck = true)

            if (has2fAStatus.isError()) {
                progressBar.hide()
                appAlert.showStatus(has2fAStatus)
                return@launch
            }

            enable2faBtn.setOnClickListener {

                val intentBundle = Bundle().apply {
                    putString("on_finished_activity", SettingActivity::class.java.name)
                }

                startClassActivity(
                        activityClass = Enable2faActivity::class.java,
                        data = intentBundle
                )

                return@setOnClickListener
            }

            progressBar.hide()

            val has2fa = has2fAStatus.getData<Boolean>() ?: false

            //if its disabled already
            if(!has2fa){
                progressBar.hide()
                toolbar.setBackgroundResource(R.color.colorPrimaryDark)
                twoFAStatus.visibility = View.VISIBLE
                return@launch
            }

            //show 2fa
            twoFaDataView.show()

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


            //verify and save
            verifyAndSave.setOnClickListener {

                var hasError = false

                verificationCodeInputLayout.error = ""
                twoFACodeInputLayout.error = ""

                val verifCode = verificationCodeInput.text.toString()

                if (verifCode.isEmpty()) {
                    verificationCodeInputLayout.error = getString(R.string.verification_code_required)
                    hasError = true
                }

                val twoFACode = twoFACodeInput.text.toString()

                if (!twoFACode.isInt()) {
                    twoFACodeInputLayout.error = getString(R.string.twofa_code_must_be_numeric)
                    hasError = true
                }

                if (hasError) {
                    return@setOnClickListener
                }


                UI.launch {

                    appLoader.show()

                    val params = listOf<Pair<String, Any>>(
                            Pair("verification_code", verifCode),
                            Pair("twofa", twoFACode)
                    )

                    val save2FAStatus = TnsApi(mActivity).post(
                            requestPath = "/account/verify-and-disable-2fa",
                            params = params,
                            hasAuth = true
                    )

                    appLoader.hide()

                    appAlert.showStatus(save2FAStatus)

                    //close if success
                    if (save2FAStatus.isError()) {
                        return@launch
                    }


                    Account(mActivity).updateLocalUserInfo("has_2fa", false)

                    delay(2000)

                    //nagivate back
                    startClassActivity(onFinishedActivity, true)

                }//end launch

            }//end onclick listener

        } //end launch

    }//end fun



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
