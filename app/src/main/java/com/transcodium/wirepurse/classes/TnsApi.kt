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
#  created_at 29/07/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import com.transcodium.wirepurse.*
import okhttp3.Response
import org.json.JSONObject
import java.util.*
import kotlin.Exception


class TnsApi(val context: Context){

    private var retryUIShowing = false

    var requestResults: Status? = null


     //dont forget, call it from only activity based context
     private val rootView by lazy{
        (context as Activity).window.decorView.rootView as ViewGroup
     }

     private val appAlert by lazy {
        AppAlert((context as Activity))
     }

    private val pb by lazy {
        Progress((context as Activity))
    }

     /**
      * requestHeaders

      */
      private val getRequestHeaders by lazy{

            val appName = context.getString(R.string.app_name)
            val appVersion = BuildConfig.VERSION_NAME
            val serviceId =  "wallet"

            val lang = Locale.getDefault().language

            val deviceName = Build.MODEL
            val deviceMan =  Build.MANUFACTURER ?: ""
            val deviceBuild = Build.DEVICE
            val osVer = Build.VERSION.RELEASE

            val userAgent = "$appName $serviceId/$appVersion (Linux; Android $osVer; $deviceMan $deviceName )"

            //Log.e("USER_AGENT",userAgent)

             mutableMapOf(
                    "x-device-id"     to  getDeviceId(context),
                    "x-platform"      to  "android",
                    "x-service-name"  to  serviceId,
                     "x-product-id"   to  "wirepurse",
                    "x-api-key"       to  API_KEY,
                    "User-Agent"      to  userAgent,
                    "Accept-Language" to  lang
            )
        }


        /**
        * RequestAuth
        **/
        private fun getRequestAuth(
                addToHeaders: Boolean? = true
        ): Status{

            //lets get user auth info
            val authInfo = SecureSharedPref(context)
                      .getJSONObject(USER_AUTH_INFO)

            if(authInfo == null){
               return errorStatus(message = R.string.auth_not_found, isSevere = true)
            }

            //fetch the access token from the file
            val accessTokenStatus = getAccessToken(authInfo)


            if(accessTokenStatus.isError()){
                return accessTokenStatus
            }//end if error


            //get access token
            val accessToken = accessTokenStatus.getData<String>()

            val userId = authInfo.optString("user_id","")

           //add access token
            authInfo.put("access_token",accessToken!!)

            //if attach to headers is true
            if(addToHeaders!!){
                getRequestHeaders["x-user-id"] = userId
                getRequestHeaders["Authorization"] = "Bearer $accessToken"
            }

            return successStatus(data = authInfo)
        }


        /**
         * post
         */
        suspend fun post(
                requestPath: String,
                params: List<Pair<String,Any>>? = null,
                headers: MutableMap<String,String>? = null,
                hasAuth: Boolean? = true,
                onRetry:  (()-> Unit)? = null
        ): Status {

             return processAPIRequest(
                     requestType = "post",
                     requestPath =requestPath,
                     params = params,
                     headers = headers,
                     hasAuth = hasAuth,
                    onRetry = onRetry
            )

        }//end fun

    /**
     * get
     */
    suspend fun get(
            requestPath: String,
            params: List<Pair<String,Any>>? = null,
            headers: MutableMap<String,String>? = null,
            hasAuth: Boolean? = true,
            onRetry:  (()-> Unit)? = null
    ): Status {

         return processAPIRequest(
                requestType = "get",
                requestPath =requestPath,
                params = params,
                headers = headers,
                hasAuth = hasAuth,
                onRetry = onRetry
        )

    }//end fun


    /**
     * proccessApiRequest
     */
    private suspend fun processAPIRequest(
            requestType: String,
            requestPath: String,
            params: List<Pair<String,Any>>? = null,
            headers: MutableMap<String,String>? = null,
            hasAuth: Boolean? = true,
            onRetry:  (()-> Unit)? = null
    ): Status {

        //if no network available
        if (!context.hasNetworkConnection()){
            return errorStatus(R.string.network_not_available)
        }

        val requestHeaders =  getRequestHeaders

        if(headers != null && headers.isNotEmpty()){
            requestHeaders.putAll(headers)
        }

        //Log.e("LOL",JSONObject(requestHeaders).toString())

        //if request hasAuth
        if(hasAuth!!){
            val requestAuthStatus = getRequestAuth(true)

            if(requestAuthStatus.isError()){
                return requestAuthStatus
            }
        }//end if request has auth

        //Log.e("LOL",JSONObject(requestHeaders).toString())

        val webClient = WebClient()

        val url = "$API_ENDPOINT/$requestPath"

        val requestStatus = if(requestType == "get"){

            webClient.getRequestAsync(
                    url = url,
                    headers = requestHeaders,
                    params = params
            )

        }else{

            webClient.postRequestAsync(
                    url = url,
                    headers = requestHeaders,
                    params = params
            )
        }//end else

        //val requestStatus = deferredRequestStatus.await()
        //println("===============>>>> Status: -> ${requestStatus.toJsonString()}")
        if(requestStatus.isError()){
            return requestStatus
        }


        return try {

           // println("-----> Status  ${requestStatus.toJsonString()}")
            /**
             * Sometimes, strings are detected as JSONObject,
             * so we will detect any errors abd cast to string if not string
             */
            val httpResponse = requestStatus.getData<Response>()

            val responseBody = httpResponse?.body()

            if(responseBody == null || responseBody.contentLength() == 0L){
                Log.e("TNS_API_ERRROR","Empty Data Returned")
                return errorStatus(R.string.unexpected_error)
            }

            val responseData = String(responseBody.bytes(),Charsets.UTF_8)

            responseData.toStatus()

        }catch(e: Exception){

            e.printStackTrace()

            println("-----> Result  ${requestStatus.toJsonString()}")

            if(requestStatus is Status){
                requestStatus
            } else {
                errorStatus(R.string.unexpected_error)
            }
        }

    }//end fun

    /**
     * getAccessToken Check if access token has expired, if it has expired
     * regenerate before send it back
     */
     fun getAccessToken(
            authInfo : JSONObject
    ): Status {


        val tokenData: JSONObject? = authInfo.optJSONObject("token_data")

        if(tokenData == null){
            return errorStatus(
                    message = R.string.auth_required,
                    isSevere = true
            )
        }


        val expiry = tokenData.optInt("expiry",0)

        val accessToken = tokenData.optString("token","")

        if(accessToken.isEmpty()){
            return errorStatus(
                    message = R.string.auth_required,
                    isSevere = true
            )
        }


        //lets get current time in milliseconds
        val now = Calendar.getInstance().timeInMillis


        if(expiry > now){
            return successStatus(data = accessToken)
        }

        //if we here,, then lets update access token
        return successStatus(data = accessToken)
    }//end

}