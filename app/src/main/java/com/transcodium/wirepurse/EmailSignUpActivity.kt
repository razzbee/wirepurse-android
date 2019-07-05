package com.transcodium.wirepurse

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_email_sign_up.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.NumberFormatException


class EmailSignUpActivity : AppCompatActivity() {


    private val mActivity by lazy {
        this
    }

    private val appAlert by lazy {
        AppAlert(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sign_up)



        backfab.setOnClickListener {
            mActivity.onBackPressed()
        }

        loginBtn.setOnClickListener {
            startClassActivity(LoginActivity::class.java,true)
        }


        requestCodeBtn.setOnClickListener {
            IO.launch {

                val email = emailAddressInput.text.toString()

                if(email.isEmpty() || !email.isValidEmail()){
                    appAlert.error(R.string.valid_email_required)
                    return@launch
                }

                VerificationCode(mActivity).sendCode(
                        emailAddress = email,
                        activityGroup = "signup",
                        hasAuth = false
                )
            }
        }


        submitEmailSignup.setOnClickListener {
            processEmailSignup()
        }
    }//end fun



    /**
     * processEmailLogin
     */
    private fun processEmailSignup() = UI.launch {

        var hasError = false

        fullNameInputLayout.error = ""
        emailAddressInputLayout.error = ""
        verificationCodeInputLayout.error = ""
        passwordInputLayout.error = ""
        confrimPasswordInputLayout.error = ""


        val fullName = fullNameInput.text.toString()

        if(fullName.isEmpty()){
            fullNameInputLayout.error = getString(R.string.full_name_is_required)
            hasError = true
        }


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

        val password = passwordInput.text.toString()

        if (password.isEmpty()) {
            passwordInputLayout.error = getString(R.string.password_required)
            hasError = true
        }

        val passwordConfirm = confrimPasswordInput.text.toString()

        if(password.compareTo(passwordConfirm) != 0){
            confrimPasswordInputLayout.error = getString(R.string.passwords_do_not_match)
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


        val processSignUpStatus = Account(mActivity)
                    .processEmailSignup(
                            name = fullName,
                            email = emailAddress,
                            password = password,
                            confirmPassword = passwordConfirm,
                            verificationCode = verificationCodeStr
                    )

        progress.hide()

        //show status
        appAlert.showStatus(processSignUpStatus)

        if(processSignUpStatus.isError()){
            return@launch
        }

        delay(1000)

        val i = Intent(mActivity,PinCodeAuthActivity::class.java)

        //start pincode request
        startActivityForResult(i,INAPP_AUTH_REQUEST_CODE)

    }//end fun



    /**
     * onActivityResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        //if its pin code activity auth
        if (!(requestCode == INAPP_AUTH_REQUEST_CODE &&
                        resultCode == Activity.RESULT_OK)) {
            return
        }

        val status = data?.getStatus()!!


        if (status.isSuccess()) {
            startClassActivity(HomeActivity::class.java, true)
            return
        }//end

    }//end fun


}//end activity class
