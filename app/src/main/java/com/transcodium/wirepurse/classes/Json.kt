package com.transcodium.wirepurse.classes

import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject


fun Map<*,*>.toJSONObject(): JSONObject{
    return JSONObject(this)
}


fun JSONArray.size(): Int = length()

fun JSONObject.size(): Int = length()

fun JSONObject.isEmpty(): Boolean = length() == 0

fun JSONArray.isEmpty(): Boolean =  length() == 0

fun JSONArray.indexRange(): IntRange {
    return if(isEmpty()){
        IntRange.EMPTY
    } else {
        IntRange(0,length() - 1)
    }
}

fun JSONArray.indexRangeReverse(): IntProgression {
    return if(isEmpty()){
        0.downTo(0)
    } else {
        (length() - 1).downTo(0)
    }
}


/**
 * JSONArray to List
 **/
fun <T> JSONArray.toMutableList(): MutableList<T>{

    val JSONArrayData = this

    val processedData = mutableListOf<T>()

    for(key in JSONArrayData.indexRange()){

        val item = JSONArrayData[key] as T

        processedData.add(item)
    }

    return processedData
} //end fun
