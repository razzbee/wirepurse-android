package com.transcodium.wirepurse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.transcodium.wirepurse.classes.Account
import com.transcodium.wirepurse.classes.AppAlert
import com.transcodium.wirepurse.classes.Progress
import com.transcodium.wirepurse.classes.VerificationCode
import kotlinx.android.synthetic.main.activity_reset_password.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ResetPasswordActivity : AppCompatActivity() {


    private val mActivity by lazy {
        this
    }

    private val appAlert by lazy {
        AppAlert(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        backfab.setOnClickListener {
            mActivity.onBackPressed()
        }

        submitResetPassword.setOnClickListener {
            processResetPassword()
        }


        //Request verif code
        requestCodeBtn.setOnClickListener {
            IO.launch {

                val email = emailAddressInput.text.toString()

                if(email.isEmpty() || !email.isValidEmail()){
                    appAlert.error(R.string.valid_email_required)
                    return@launch
                }

                VerificationCode(mActivity).sendCode(
                        emailAddress = email,
                        activityGroup = "reset_password",
                        hasAuth = false
                )
            }
        } //end fun


    }


    /**
     * processResetPassword
     */
    private fun processResetPassword() = UI.launch {

        var hasError = false

        emailAddressInputLayout.error = ""
        verificationCodeInputLayout.error = ""
        newPasswordInputLayout.error = ""
        confrimNewPasswordInputLayout.error = ""


        val emailAddress = emailAddressInput.text.toString()

        //validate email
        if (!emailAddress.isValidEmail()) {
            emailAddressInputLayout.error = getString(R.string.valid_email_required)
            hasError = true
        }

        val verificationCodeStr = verificationCodeInput.text.toString()

        if(verificationCodeStr.isEmpty()){
            verificationCodeInputLayout.error = getString(R.string.verification_code_required)
            hasError = true
        }

        try{
            Integer.parseInt(verificationCodeStr)
        } catch (e: NumberFormatException){
            verificationCodeInputLayout.error = getString(R.string.verification_code_must_be_numeric)
            hasError = true
        }

        val newPassword = newPasswordInput.text.toString()

        if (newPassword.isEmpty()) {
            newPasswordInputLayout.error = getString(R.string.password_required)
            hasError = true
        }

        val confirmNewPassword = confrimNewPasswordInput.text.toString()

        if(newPassword.compareTo(confirmNewPassword) != 0){
            confrimNewPasswordInputLayout.error = getString(R.string.passwords_do_not_match)
            hasError = true
        }

        //dont continue if error
        if (hasError) {
            vibrate()
            return@launch
        }


        val progress = Progress(mActivity)

        progress.show(
                title = R.string.loading,
                text = R.string.signup_progress_text,
                bgColor = R.color.purple
        )




        val status = Account(mActivity).processResetPassword(
                email = emailAddress,
                newPassword = newPassword,
                confirmNewPassword = confirmNewPassword,
                verificationCode = verificationCodeStr
        )

        progress.hide()

        appAlert.showStatus(status)

        if(status.isSuccess()){

            delay(2000)

            startClassActivity(EmailLoginActivity::class.java)
        }
    }//end fun


}//en class
