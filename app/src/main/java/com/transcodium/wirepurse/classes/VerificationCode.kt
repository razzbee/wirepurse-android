/**
# Copyright 2018 - Transcodium Ltd.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the  Apache License v2.0 which accompanies this distribution.
#
#  The Apache License v2.0 is available at
#  http://www.opensource.org/licenses/apache2.0.php
#
#  You are required to redistribute this code under the same licenses.
#
#  Project TNSMoney
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 15/08/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.transcodium.wirepurse.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.coroutines.launch
import org.json.JSONObject


class VerificationCode(val activity: Activity) {

    val appAlert by lazy{
        AppAlert(activity)
    }

    /**
     * sendCode
     */
      suspend fun sendCode(
            emailAddress: String? = "",
            userId: String? = "",
            activityGroup: String,
            hasAuth:Boolean? = true
    ): Status{

        if(emailAddress.isNullOrEmpty()  && hasAuth != true){
            Log.e("Verif_ERROR","Email cannot be null and hasAuth cannot be false, kindly provide one")
            appAlert.error(activity.getString(R.string.unexpected_error))
            return errorStatus(R.string.unexpected_error)
        }

        var userEmail = emailAddress

        if(emailAddress.isNullOrEmpty()){

            val userInfo = activity.getUserInfo()!!

            userEmail = userInfo.getString("user_email")
        }

        val recaptchaStatus = SafeNetCore().verifyWithRecaptcha(activity)

        if(recaptchaStatus.isError()){
            appAlert.error(recaptchaStatus.getMessage(activity))
            return recaptchaStatus
        }

        val recaptchaToken = recaptchaStatus.getData<String>()

        val p = Progress(activity)

        p.show(
                text = R.string.resending_verification_code,
                bgColor = R.color.colorPrimaryDark
        )


        val uri = "/auth/verification-code/send"

        val params = listOf(
                Pair("user_id",userId as Any),
                Pair("email",userEmail as Any),
                Pair("group",activityGroup as Any),
                Pair("g-recaptcha-response",recaptchaToken as Any)
        )

        val resendStatus = TnsApi(activity)
                .post(
                   requestPath = uri,
                   hasAuth = hasAuth!!,
                   params = params
                )


        p.hide()

       if(resendStatus.isError()){
           appAlert.error(resendStatus.getMessage(activity))
           return resendStatus
       }

        //lets get validation data and reshow the count down
        val verificationData = resendStatus.getData<JSONObject>()

        if(verificationData != null) {
            processWaitTime(verificationData)
        }

        if(resendStatus.isError()){
            AppAlert(activity).showStatus(resendStatus)
            return resendStatus
        }


        AppAlert(activity).success(R.string.code_sent_to_your_email,true)

        return resendStatus

    }//end fun




    /**
     * resend
     */
    suspend fun resendCode(dbId: String,hasAuth:Boolean? = true): Status {

        val p = Progress(activity)

        p.show(
                text = R.string.resending_verification_code,
                bgColor = R.color.colorPrimaryDark
        )


        val uri = "/auth/verification-code/$dbId/resend"
        val params = listOf(Pair("id",dbId))

        val resendStatus = TnsApi(activity)
                .post(
                        requestPath = uri,
                        params = params,
                        hasAuth = hasAuth!!
                )


        p.hide()

        if(resendStatus.isError()){
            AppAlert(activity).showStatus(resendStatus)
            return resendStatus
        }

        AppAlert(activity).success(R.string.code_sent_to_your_email,true)

        //lets get validation data and reshow the count down
        val verificationData = resendStatus.getData<JSONObject>()!!

        processWaitTime(verificationData)

        return resendStatus
    }//end fun


    /**
     * proccessVerificationCallback
     */
    private fun processWaitTime(data: JSONObject) = UI.launch{

        val waitTime = data.optLong("wait_time",0L)


        //Log.i("Wait Time",waitTime.toString())

        val waitTimeTextView = activity.waitTimeTextView
        val requestCodeBtn =  activity.requestCodeBtn

        if(waitTime <= 0){

            waitTimeTextView?.visibility = View.GONE
            requestCodeBtn?.visibility = View.VISIBLE

        }else{

            //for now set resend to hideen
            waitTimeTextView?.visibility = View.VISIBLE
            requestCodeBtn?.visibility = View.GONE

            //1000 ms = 1 second
            val countDownInterval = 1000L

            //convert seconds to milliseconds
            val waitTimeMs = waitTime * countDownInterval

            val waitTimeText =  activity.getString(R.string.resend_after)

            val countDown = object: CountDownTimer(waitTimeMs,countDownInterval){

                override fun onFinish() {

                    waitTimeTextView?.visibility = View.GONE
                    requestCodeBtn?.visibility = View.VISIBLE

                }

                override fun onTick(time: Long) {

                    val t =  "$waitTimeText ${time / countDownInterval}"

                    waitTimeTextView?.text = t

                    // Log.i("TICK",time.toString())
                }
            }//end coundown timer

            countDown.start()
        }//end if

    }//end fun


}//end class