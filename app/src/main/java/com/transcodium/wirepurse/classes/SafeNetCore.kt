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
#  created_at 19/08/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetStatusCodes.*
import com.transcodium.wirepurse.GOOGLE_RECAPTCHA_SITE_KEY
import com.transcodium.wirepurse.R
import com.transcodium.wirepurse.awaitEvent

class SafeNetCore {


    /**
     * get request error
     */
    private fun getRecaptchaError(errorCode: Int): Int{

        return when(errorCode){
            RECAPTCHA_INVALID_SITEKEY -> R.string.invalid_recaptcha_key
            RECAPTCHA_INVALID_KEYTYPE  -> R.string.invalid_recaptcha_key_type
            RECAPTCHA_INVALID_PACKAGE_NAME -> R.string.recaptcha_invalid_package
            UNSUPPORTED_SDK_VERSION -> R.string.recaptcha_unsupported_sdk_version
            TIMEOUT -> R.string.recaptcha_timeout
            NETWORK_ERROR -> R.string.recaptcha_network_error
            CommonStatusCodes.ERROR -> R.string.recaptcha_general_error
            else ->  R.string.recaptcha_general_error
        }
    }//end fun

    /**
     * verifyRecaptcha
     */
    suspend fun verifyWithRecaptcha(activity: Activity): Status {

        return awaitEvent{ h ->
            SafetyNet.getClient(activity).verifyWithRecaptcha(GOOGLE_RECAPTCHA_SITE_KEY)
                    .addOnSuccessListener { tokenResponse->
                        h.handle(successStatus(data = tokenResponse.tokenResult))
                    }
                    .addOnFailureListener{e->

                        if(e is ApiException) {

                            val statusCode = e.statusCode

                            val errMessage = CommonStatusCodes.getStatusCodeString(e.statusCode)

                            Log.e("Recaptcha Error:",errMessage)

                            val msg = activity.getString(getRecaptchaError(statusCode))

                            h.handle(errorStatus(msg))

                        } else {

                            Log.e("Recaptcha Error:",e.message)

                            h.handle(errorStatus(activity.getString(R.string.recaptcha_general_error)))
                        }

                        e.printStackTrace()

                    }//end on error
        }//end fun

    } //end fun



}//end class