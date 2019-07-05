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
#  created_at 27/07/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.room.Room
import com.google.gson.JsonObject
import com.transcodium.wirepurse.*
import com.transcodium.wirepurse.db.AppDB
import kotlinx.coroutines.launch
import org.jetbrains.anko.commit
import org.json.JSONObject

import java.io.File
import java.util.*
import kotlin.reflect.KClass


class Account(private val mContext: Context) {


    //if user is logged in
    fun isLoggedIn(): Boolean {


        val authInfoJson = mContext.secureSharedPref()
                                .getJSONObject(USER_AUTH_INFO)
        ?: return false


        if(!(authInfoJson.has("user_id") ||
           authInfoJson.has("email") ||
           authInfoJson.has("token_data")
        )){
            return false
        }//end if

        return true
    }//end


    /**
     * process Login
     */
    suspend fun processEmailLogin(
         email: String,
         password: String
    ): Status {


        val loginParam = listOf(
                Pair("email",email as Any),
                Pair("password",password as Any)
        )

        //process login
        val loginStatus = TnsApi(mContext).post(
                requestPath = "/auth/login",
                params = loginParam,
                hasAuth = false

        )

        //println(loginStatus.toJsonString())

        if(loginStatus.isError()){
            return loginStatus
        }

        //lets save the login info
        val data = loginStatus.getData<JSONObject?>()

        if(data == null){
            return errorStatus(
                    message = R.string.unexpected_error,
                    code =  StatusCodes.EMAIL_LOGIN_DATA_NULL
            )
        }

        //save data
        saveUserInfo(data)

        return successStatus(message = R.string.login_success)
    }//end


    /**
     * processEmailSignup
     */
    suspend fun processEmailSignup(
            name: String,
            email: String,
            password: String,
            confirmPassword: String,
            verificationCode: String
    ): Status {

        val timezone = TimeZone.getDefault().id

        val params = listOf(
                Pair("email",email),
                Pair("name", name),
                Pair("password",password),
                Pair("confirm_password",confirmPassword),
                Pair("verification_code",verificationCode),
                Pair("timezone",timezone)
        )

        val processSignUpStatus = TnsApi(mContext).post(
                requestPath = "/auth/signup",
                params = params,
                hasAuth = false
        )

        if(processSignUpStatus.isError()){
            return processSignUpStatus
        }

        val data = processSignUpStatus.getData<JSONObject?>()
            ?:  return errorStatus(
                    message = R.string.unexpected_error,
                    code =  StatusCodes.EMAIL_SIGNUP_DATA_NULL
            )


        //save data
        saveUserInfo(data)

        return successStatus(message = R.string.signup_success)
    }//end fun


    /**
     * processEmailSignup
     */
    suspend fun processResetPassword(
            email: String,
            newPassword: String,
            confirmNewPassword: String,
            verificationCode: String
    ): Status {


        val params = listOf(
                Pair("email",email),
                Pair("new_password",newPassword),
                Pair("confirm_new_password",confirmNewPassword),
                Pair("verification_code",verificationCode)
        )

        val processSignUpStatus = TnsApi(mContext).post(
                requestPath = "/auth/reset-password",
                params = params,
                hasAuth = false
        )

        return processSignUpStatus
    }//end fun


    /**
     * save UserInfo
     */
    fun saveUserInfo(userInfo: JSONObject): Status{

       if(userInfo.has("email")){
           userInfo.put("user_email",userInfo.getString("email"))
       }

       //println("UserIfo - $userInfo")
       return mContext.secureSharedPref().put(USER_AUTH_INFO,userInfo)
    }//end fun


    /**
     * clearUserData
     */
     fun clearUserData() = launchIO {


        mContext.sharedPref().edit{
            remove(USER_AUTH_INFO)
        }

        val db = AppDB.getInstance(mContext)

        //clear all the data
        db.userAssetsDao().deleteAll()
        db.assetAddressDao().deleteAll()

    }//end fun

    /**
     * doLogout
     */
    fun doLogout(
        status: Status? = null
    ){

        val userInfo = mContext.getUserInfo()

        //clear db
        clearUserData()

        var bundle: Bundle? = null

        if(status != null){

            Log.e("LOGOUT_STATUS",status.toJsonString())

            bundle = Bundle()
            bundle.putString("status",status.toJsonString())
        }

       //if ccntext isnt activity, means we dont have UI attached
      if(mContext !is Activity){ return }

        mContext.startClassActivity(
                activityClass = LoginActivity::class.java,
                clearActivityStack = true,
                data = bundle
        )

        if(userInfo == null){ return }

        //check if have social name and not empty
        val socialName = userInfo.optString("social_name",null) ?: return

        if(socialName == "google"){
           SocialLoginCore(mContext).logoutGoogleAccountAsync()
        } else if(socialName == "twitter"){
            SocialLoginCore(mContext).logoutTwitterAccountAsync()
        }

    } //end fun


    /**
     * updateAccount
     */
    suspend fun updateAccount(
       fullName: String,
       verificationCode: String
    ): Status {

        val savedUserData = mContext.getUserInfo()!!

        val dataToUpdate = listOf(
                Pair("name",fullName),
                Pair("verification_code",verificationCode)
        )

        val updateStatus = TnsApi(mContext).post(
                requestPath = "/account/update",
                params = dataToUpdate,
                hasAuth = true
        )

        if(updateStatus.isError()){
            return updateStatus
        }

        //update user data now
        savedUserData.put("user_full_name",fullName)

        //resave the data
        saveUserInfo(savedUserData)

        return updateStatus
    }//end fun


    /**
     * renewExpiredAccessToken
     */
    suspend fun renewExpiredAccessToken(force: Boolean = false){

        if(!isLoggedIn()){
            return
        }

        //renewing expired access token
        val userInfo = mContext.getUserInfo()!!

       // println("--->>>>> $userInfo")
    }//end


    /**
     * has2fa
     */
    suspend fun has2FA(doRemoteCheck: Boolean? = false): Status {

        var userInfo: JSONObject? = null

        //lets check if user has setup 2fa
        if(doRemoteCheck!!){
            val userInfoStatus = getRemoteAccountInfo()

            if(userInfoStatus.isError()){
                return userInfoStatus
            }

             userInfo = userInfoStatus.getData<JSONObject>()
        } else {
            userInfo = mContext.getUserInfo()
        }

        if(userInfo == null){
            return errorStatus(R.string.unexpected_error)
        }


        val has2FA = userInfo.optBoolean("has_2fa",true)

        return successStatus(data = has2FA)
    }//end fun


    suspend fun getRemoteAccountInfo(): Status {

        //lets get old user data
        val userInfo = mContext.getUserInfo()
                ?: return errorStatus(R.string.unexpected_error)

        //get user info
        val accountInfoStatus = TnsApi(mContext).get(
                requestPath = "/account/info",
                hasAuth = true
        )

        if(accountInfoStatus.isError()){
            return accountInfoStatus
        }

        //account Data
        val accountData = accountInfoStatus.getData<JSONObject?>()

        if(accountData == null){

            val extraParam = Bundle().apply {
                putString("user_id",userInfo.getString("user_id"))
                putString("user_email",userInfo.getString("user_email"))
            }


            mContext.fireLog("getRemoteAccountInfo","remote data .getData() is null",extraParam)
            return errorStatus(R.string.unexpected_error)
        }//end if

        //lets merge json
        userInfo.merge(accountData)

        val saveStatus = saveUserInfo(userInfo)

        if(saveStatus.isError()){
            return saveStatus
        }

        return successStatus(data = userInfo)
    }//end fun


    /**
     * updateUserInfo
     */
    fun updateLocalUserInfo(key: String, value: Any): Status{

        //lets get old user data
        val userInfo = mContext.getUserInfo()
                ?: return errorStatus(R.string.unexpected_error)

        userInfo.put(key,value)

        val saveStatus = saveUserInfo(userInfo)

        if(saveStatus.isError()){
            return saveStatus
        }

        return successStatus()
    } //end


    /**
     * handlePostLogin
     */
    fun handlePostLogin(
            activity: Activity,
            checkTwoFa: Boolean = true
    ) = UI.launch {

        var activityClass: KClass<*> = HomeActivity::class

        if(checkTwoFa) {

            //val pb =  Progress(activity)

            //pb.show(title = R.string.checking_2fa_status)

            val has2FAStatus = has2FA(false)

            if (has2FAStatus.isError()) {
                doLogout(has2FAStatus)
                return@launch
            }

            val has2FA = has2FAStatus.getData<Boolean>()!!

            //lets check if user has 2fa
            if (has2FA) {
                activityClass = TwoFaAuthView::class
            }
        } //end if check 2fa

        val mActivity = mContext as Activity

        mActivity.startClassActivity(activityClass.java, true)
    }//end fun



}//end class