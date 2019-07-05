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
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.content.Context
import org.json.JSONObject

enum class StatusTypes
{
    success,
    error,
    neutral
}

/**
 * Status after a function call
 */
data class  Status (
        val  type: StatusTypes,
        val  message: Any? = "",
        val  data: Any? = null,
        val  code: Int? = 0,
        val  isSevere: Boolean? = false
){

    //isError
    fun isError(): Boolean{
        return type ==  StatusTypes.error
    }

    //succeeded
    fun isSuccess(): Boolean{
        return  type == StatusTypes.success
    }

    fun isNeutral(): Boolean{
        return  type == StatusTypes.neutral
    }


    fun isSevere(): Boolean{
        return  isSevere ?: false
    }

    fun  data(): Any? {
        return data
    }

    fun message(mContext: Context? = null) : String {

        if(mContext == null || message is String){
          return message.toString()
       }
       else if(message is Int){
           return mContext.getString(message)
       } else {
            return message.toString()
        }
    }

    fun getResMessage(): Int{
        return message as Int
    }

    /*
     * toJson
     */
    fun toJsonString(): String {
        return toJsonObject().toString()
    }

    /**
     * to Json Object
     */
    fun toJsonObject(): JSONObject {
        return JSONObject(mapOf(
                "type" to type.name,
                "message" to message,
                "code" to code,
                "data" to data,
                "is_severe"  to isSevere
        ))
    }//end


    /**
     * toString
     */
    override fun toString(): String{
        return toJsonObject().toString()
    }

}


/**
 * Helper Funs
 **/
@Synchronized fun successStatus(
    message: Any? = "",
    data: Any?  = null,
    code: Int? = 0
): Status{
    return Status(
            type = StatusTypes.success,
            data = data,
            code = code!!,
            message = message!!
    )
}

/**
 * Helper Funs
 **/
@Synchronized fun successStatus(
        data: Any?  = null
): Status {
    return Status(
            type = StatusTypes.success,
            data = data
    )
}



//Error
@Synchronized fun errorStatus(
        message: Any? = "",
        data: Any? = null,
        code: Int? = 0,
        isSevere: Boolean = false
): Status {
    return Status(
            type = StatusTypes.error,
            message = message!!,
            data = data,
            code = code!!,
            isSevere = isSevere
    )
}


//neutral
@Synchronized fun neutralStatus(
        message: Any? = "",
        data: Any? = null
): Status {
    return Status(
            type = StatusTypes.neutral,
            message = message!!,
            data = data,
            code = StatusCodes.NEUTRAL
    )
}




@Synchronized fun  jsonToStatus(data: JSONObject): Status {

    val type = data.getString("type")

    val statusType = when(type){
        "success" -> StatusTypes.success
        "error"  -> StatusTypes.error
        "neutral" -> StatusTypes.neutral

        else -> {
            return errorStatus("unexpected_error")
        }
    }

    return Status(
            type = statusType,
            message =  (data.get("message") ?: "") as Any,
            data = data.get("data") as Any?,
            code = data.optInt("code",0),
            isSevere =  data.optBoolean("is_severe",false)
    )
}


fun JSONObject.toStatus(): Status {
    return jsonToStatus(this)
}


fun String.toStatus(): Status {

    val jsonObj = try{
        JSONObject(this)
    } catch (e: Exception){
        return errorStatus("uexpected_error")
    }

    return jsonToStatus(jsonObj)
}


@Suppress("UNCHECKED_CAST")
@Synchronized fun <T> Status.getData(): T? {
    return this.data as T?
}

@Synchronized fun Status.getMessage(mContext: Context? = null): String{
    return this.message(mContext)
}