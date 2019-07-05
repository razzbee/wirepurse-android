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
#  created_at 16/08/2018
 **/

package com.transcodium.wirepurse.classes

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson

import com.transcodium.wirepurse.sharedPref
import org.json.JSONArray

import com.transcodium.wirepurse.R
import org.json.JSONObject
import java.math.BigInteger


class SecureSharedPref(val context: Context) {

    val sharedPref = context.sharedPref()

    val crypt = Crypt()


    /**
     * put
     */
    fun put(key: String, value: Any): Status {

        val rawData = when(value){
            is JSONObject ->  value.toString()
            is JSONArray ->   value.toString()
            else -> value
        }

        val encryptDataStatus = crypt.encrypt(context,rawData)

        if(encryptDataStatus.isError()){

            Log.e("DATA_ENCRYPT_ERR",encryptDataStatus.getMessage())

            //lets check reason
            return encryptDataStatus
        }

        val encryptedData = encryptDataStatus.getData<String>()

        sharedPref.edit{
            putString(key, encryptedData)
        }

        return successStatus()
    }//end


    /**
     * get
     */
    private fun get(key: String): ByteArray?{

      //lets check if exists
      val data = sharedPref.getString(key,"")

        //Log.e("KEY ---  $key",data)

        if(data.isNullOrEmpty()){
            return null
        }

        //lets decrypt data
        val decryptDataStatus = crypt.decrypt(context,data!!)

        if(decryptDataStatus.isError()){
            Log.e("DATA_DECRYPT_ERR",decryptDataStatus.getMessage())
            return null
        }

        return try{
             decryptDataStatus.getData<ByteArray>()!!
        } catch(e: Exception){
            e.printStackTrace()
            null
        }

    }//end


    /**
     * getString
     */
    fun getString(key: String,def : String? = null): String?{

        val data = get(key) ?: return def

        return String(data)
    }//emd


    /**
     * getInt
     */
    fun getInt(key: String,def : Int? = null): Int?{

        val data = get(key) ?: return def

        return BigInteger(data).toInt()
    }//end


    /**
     * getFloat
     */
    fun getFloat(key: String,def : Float? = null): Float?{

        val data = get(key) ?: return def

        return BigInteger(data).toFloat()
    }//end

    /**
     * getDouble
     */
    fun getDouble(key: String,def : Double? = null): Double?{

        val data = get(key) ?: return def

        return BigInteger(data).toDouble()
    }//end


    /**
     * getDouble
     */
    fun getJSONObject(key: String,def : JSONObject? = null): JSONObject?{

        val data = get(key) ?: return def

        val dataStr = String(data)

        return try{
            JSONObject(dataStr)
        }catch (e: Exception){
            e.printStackTrace()
            def
        }

    }//end

}