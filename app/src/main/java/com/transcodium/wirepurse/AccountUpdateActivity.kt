package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_account_update.*
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.longToast

class AccountUpdateActivity : AppCompatActivity() {

    private val mActivity  by lazy { this }

    private val userInfo by lazy { getUserInfo()!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_update)

        //on close
        closeModal.setOnClickListener {
            this.finish()
        }

        dialogTitle.text = getString(R.string.account_setting)


        val userFullName = userInfo.getString("user_full_name")

        fullNameInput.setText(userFullName)

        requestCodeBtn.setOnClickListener {
            IO.launch {

                val userEmail = userInfo.getString("user_email")

                VerificationCode(mActivity).sendCode(
                        emailAddress = userEmail,
                        activityGroup = "account_update",
                        hasAuth = true
                )
            }
        }//end onclick


        //on save
        saveAccountUpdateBtn.setOnClickListener {
            processUpdateAccount()
        }
    }//end oncreate


    /**
     * process update account
     */
     fun processUpdateAccount() = UI.launch{

        var hasError = false;

        fullNameInputLayout.error = ""
        verificationCodeInputLayout.error = ""

        val fullName = (fullNameInput.text ?: "").toString()

        if(fullName.isEmpty()){
            fullNameInputLayout.error = getString(R.string.full_name_is_required)
            hasError = true
        }

        val verificationCode =  (verificationCodeInput.text ?: "").toString()

        if(verificationCode.isEmpty()){
            verificationCodeInputLayout.error = getString(R.string.verification_code_required)
            hasError = true
        }

        if(hasError){
            return@launch
        }


        val progress = Progress(mActivity)

        progress.show(
                title = R.string.loading,
                text = R.string.saving_data,
                bgColor = R.color.amber700
        )

        val saveUpdateStatus = Account(mActivity).updateAccount(
                fullName = fullName,
                verificationCode = verificationCode
        )

        progress.hide()

        if(saveUpdateStatus.isError()) {
            AppAlert(mActivity).showStatus(saveUpdateStatus)
            return@launch
        }

        longToast(saveUpdateStatus.message(mActivity))

        //startClassActivity(SettingActivity::class.java,true)

        mActivity.finish()
    }//end fun


}//end class
