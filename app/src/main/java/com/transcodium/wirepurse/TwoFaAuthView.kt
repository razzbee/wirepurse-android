package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_two_fa_auth_view.*
import kotlinx.android.synthetic.main.twofa_view_full.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TwoFaAuthView : AppCompatActivity() {

    private val mActivity by lazy { this }
    private val account by lazy { Account(this) }
    private val appAlert by lazy { AppAlert(this) }
    private val appLoader by lazy { Progress(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_fa_auth_view)

        logoutBtn.setOnClickListener {
            account.doLogout(errorStatus(message = R.string.twofa_auth_failed))
        }


        /**
         * when verify btn is clicked
         */
        verifyCodeBtn.setOnClickListener {
            processVerify2FA()
        }//end on click listener

    } //end onCreate


    /**
     * processverify2FA
     */
    fun processVerify2FA() = UI.launch {

        var hasError = false

        twoFACodeInputLayout.error = ""

        val twoFACode = twoFACodeInput.text.toString()

        if (!twoFACode.isInt()) {
            twoFACodeInputLayout.error = getString(R.string.twofa_code_must_be_numeric)
            hasError = true
        }

        if (hasError) {
            return@launch
        }

        appLoader.show()

        val params = listOf<Pair<String, Any>>(
                Pair("twofa", twoFACode)
        )


        val save2FAStatus = TnsApi(mActivity).post(
                requestPath = "/account/verify-2fa",
                params = params,
                hasAuth = true
        )

        appLoader.hide()

        appAlert.showStatus(save2FAStatus)

        if(save2FAStatus.isError()){
           return@launch
        }

        delay(2000)

        startClassActivity(
              activityClass = HomeActivity::class.java,
                clearActivityStack = true
        )
    } //end fun


}//end fun
