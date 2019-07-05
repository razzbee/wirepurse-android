package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_social_login_verification.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import org.json.JSONObject

import java.lang.Integer.parseInt

class SocialLoginVerification : AppCompatActivity() {


    lateinit var verificationData: JSONObject

    lateinit var title: String

    private val mActivity by lazy { this}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_social_login_verification)

        val bundle = intent.extras

        val dataStr =  bundle?.getString("data")

        verificationData = JSONObject(dataStr)

        //change the request to resend code
        requestCodeBtn.text = getString(R.string.resend)

        //resend code
        requestCodeBtn.setOnClickListener {

            launchUI {

                val dbId = verificationData.getString("id")

                VerificationCode(mActivity).resendCode(dbId, false)
            }
        }

        //verify
        verifyCodeBtn.setOnClickListener{ verifyCode() }

    }


    /**
     * verify code
     */
    private fun verifyCode() = launchUI{

        val appAlert = AppAlert(mActivity)

        val p = Progress(mActivity)

        p.show(
                text = R.string.verifying_code,
                bgColor = R.color.colorPrimaryDark
        )


        val codeInt = try{
             parseInt(verificationCodeInput.text.toString())
        }catch(e: NumberFormatException){
            appAlert.error(R.string.invalid_code)
            return@launchUI
        }

        val verificationId = verificationData.optString("id","")

        val uri = "/auth/social/verify"

        val postParams = listOf<Pair<String,Any>>(
                Pair("verification_id",verificationId),
                Pair("verification_code",codeInt)
        )

        val verifyStatus = TnsApi(mActivity)
                            .post(
                                    requestPath = uri,
                                    params = postParams,
                                    hasAuth = false
                            )



        p.hide()


        if(verifyStatus.isError()){
            appAlert.showStatus(verifyStatus)
            return@launchUI
        }

        //lets save the data
        val data = verifyStatus.getData<JSONObject>()!!

        val saveData = Account(mActivity).saveUserInfo(data)


        if(saveData.isSuccess()) {
            mActivity.startClassActivity(HomeActivity::class.java, true)
        }

    }//end fun

}//end class
