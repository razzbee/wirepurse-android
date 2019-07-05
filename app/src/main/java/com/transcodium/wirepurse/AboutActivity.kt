package com.transcodium.wirepurse

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }


        val appName = getString(R.string.app_name)

        val appVersion = BuildConfig.VERSION_NAME

        appNameAndVersion.text = ("$appName v$appVersion")


        val listData = listOf(


             mapOf(
                     "item" to "terms_of_service",
                     "url" to "legal/terms_of_service.html",
                     "name" to getString(R.string.terms_of_service)
             ),

             mapOf(
                 "item" to "privacy_policy",
                 "url" to "legal/privacy_policy.html",
                 "name" to getString(R.string.privacy_policy)
             ),

             mapOf(
                 "item" to "for_merchants",
                 "url" to "for_merchants.html",
                 "name" to getString(R.string.for_merchants )
             )
        )


        val dataListArray = listData.map{ dataMap->
            return@map  dataMap["name"]
        }


        val adapter = ArrayAdapter(
                this,
                R.layout.simple_list_view,
                dataListArray
        )

        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->

            //lets get the list item
            val itemInfo = listData[position]

            val url = "$APP_DOMAIN/"+itemInfo["url"]

            val name = itemInfo["name"]

            val data = Bundle().apply{
                putString("url",url)
                putString("name",name)
                putBoolean("open_links",false)
            }

            startClassActivity(
                    activityClass = WebviewActivity::class.java,
                    data = data
            )
        }
    }

}
