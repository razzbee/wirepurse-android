package com.transcodium.wirepurse

import android.annotation.TargetApi
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_webview.*
import kotlinx.android.synthetic.main.dialog_header.*
import org.jetbrains.anko.dip
import java.net.URL
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity



class WebviewActivity : AppCompatActivity() {

    val webviewObj by lazy {
        findViewById<WebView>(R.id.webview)
    }



    val mActivity by lazy { this }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        //close dialog
        closeModal.setOnClickListener { mActivity.finish() }

        //lets get params
        val data = intent.extras

        val url: String? = data.getString("url")

        if(url == null){
            Log.e("WEBVIEW_ACTIVITY_ERR","url cannot be empty")
            mActivity.finish()
            return
        }

        val openLinks = data.getBoolean("open_links",false)

        val winHeight = getWindowSize().heightPixels * 0.8

        //println("-- Win Height ${dip(winHeight) }")

        window.setLayout(
               ViewGroup.LayoutParams.MATCH_PARENT,
               winHeight.toInt()
        )

        rootView.requestLayout()

        var name = data.getString("name") ?: ""

        if(name == ""){
            val parseUrl = URL(url)
           name = parseUrl.host.split(".").get(0)
        }

        dialogTitle.text = name

        val wvSetting = webviewObj.settings

        wvSetting.javaScriptEnabled = true

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wvSetting.safeBrowsingEnabled = true
        }

        wvSetting.loadWithOverviewMode = true
        wvSetting.useWideViewPort = true

        //println("---Open Link $openLinks")

        webviewObj.webViewClient = (object: WebViewClient(){

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {

                val reqUrl = request.url.toString()

                if(openLinks) {
                    view.loadUrl(reqUrl)
                } else {
                    openLinkExternally(reqUrl)
                }

                return true
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView, requestUrl: String): Boolean {

                 if(openLinks) {
                    view.loadUrl(requestUrl)
                } else {

                     mActivity.startActivity(
                             Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                     )
                 }

                return true
            }
        })

       webviewObj.loadUrl(url)
    }


}
