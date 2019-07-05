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
#  created_at 23/09/2018
 **/

package com.transcodium.wirepurse.classes

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.firebase.jobdispatcher.Constraint
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.transcodium.wirepurse.R
import kotlinx.android.synthetic.main.chart_marker_view.view.*
import kotlinx.android.synthetic.main.home_coin_info.*
import com.github.mikephil.charting.utils.MPPointF
import org.json.JSONArray
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.*


class TNSChart(val activity: Activity) {


   private val homeChartView: LineChart by lazy{

       val c = activity.coinInfoChart

       c.setDragEnabled(true)

       c.setScaleEnabled(true)

       c.setPinchZoom(false)
       c.axisRight.isEnabled = false

       c.description.isEnabled = false
       c.setDrawMarkers(true)

       c.setNoDataText("")
       c.setDrawBorders(false)

       //disable legend
       c.legend.isEnabled = false

       return@lazy  c
   }//end

  private val timePattern by lazy {
        val p = SimpleDateFormat("hh:mm",Locale.ENGLISH)
            p.timeZone = TimeZone.getTimeZone("UTC")
      return@lazy p
  }

  private val cal by lazy {
      val c = Calendar.getInstance()
        c.timeZone = TimeZone.getDefault()
      return@lazy c
  }

    fun processHomeCoinInfoGraph(
            data: JSONArray,
            animate: Boolean? = false
    ){

        val mChart = homeChartView

        mChart.visibility = View.GONE

        val dataSize = data.size() - 1

        activity.apply{

            val entries = mutableListOf<Entry>()


            val markerData = mutableListOf<JSONObject>()

            val whiteAlpha70 = ContextCompat.getColor(activity,R.color.whiteAlpha70)
            val whiteAlpha50 = ContextCompat.getColor(activity,R.color.whiteAlpha50P)


            //using custom x value, since it needs from 0 .. dataseize
            //changing this will lead to array index error
            //so dont use the loops variable i for entry x
            var entryXvalue = 0

            for(i in dataSize downTo 0){

               // println(i)

                val dataObj = data[i] as JSONObject

               // println(dataObj)

                val price = dataObj.optDouble("price",0.0)

                if(price <= 0.0){
                    continue
                }

                val graphDateObj = dataObj.getJSONObject("date")

                val market = dataObj.optString("market","").capitalize()

                val hourMinute = "${graphDateObj.getInt("hour")}:${graphDateObj.getInt("minute")}"

                val timeObj = timePattern.parse(hourMinute)

                cal.time = timeObj

                val localTime = "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"

                markerData.add(
                        JSONObject()
                                .put("time",localTime)
                                .put("market",market)
                )

                entries.add(Entry(entryXvalue.toFloat(),price.toFloat()))

                //increment
                entryXvalue++

            }//end loop

            val dataSet = LineDataSet(entries,null)

            dataSet.disableDashedLine()

            dataSet.setDrawFilled(true)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.setCircleColor(whiteAlpha50)
            dataSet.fillColor = whiteAlpha50
            dataSet.setDrawValues(false)
            dataSet.isHighlightEnabled = true
            dataSet.color = whiteAlpha70


            val lineData = LineData(dataSet)


            mChart.data = lineData

            val xAxis = mChart.xAxis

            xAxis.setDrawAxisLine(false)
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.axisLineColor = whiteAlpha70

            xAxis.setAvoidFirstLastClipping(true)

            val marker = graphMarkerView(activity,markerData)

            marker.chartView = mChart

            mChart.marker = marker

            val axisLeft = mChart.axisLeft
            axisLeft.setDrawGridLines(false)
            axisLeft.textColor = ContextCompat.getColor(activity,R.color.whiteAlpha70)

            //if(mChart.visibility == View.GONE){
                mChart.visibility = View.VISIBLE
            //}

            if(animate!!) {
                mChart.animateX(1000)
            }else {
               mChart.invalidate()
            }
        }//ane apply activity

    }//end fun


    /**
     * markerClass
     */
    open class graphMarkerView(
            mContext: Context,
            private val extraData: MutableList<JSONObject>,
            layoutId: Int? = R.layout.chart_marker_view
    ): MarkerView(mContext,layoutId!!){

        private val priceTextView by lazy { findViewById<TextView>(R.id.priceTextView) }
        private val timeTextView by lazy { findViewById<TextView>(R.id.timeTextView) }
        private val marketTextView by lazy { findViewById<TextView>(R.id.marketTextView) }

        /**
         * refreshContent
         */
        override fun refreshContent(e: Entry, highlight: Highlight?) {

            val price = "\$${e.y}"

            val extraDataObj = extraData[e.x.toInt()]

            priceTextView.text = price

            timeTextView.text = extraDataObj.optString("time","")

            marketTextView.text = extraDataObj.optString("market","")

             super.refreshContent(e, highlight)
        }//end fun


        override fun getOffset(): MPPointF {
            return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
        }

    }

}//end class