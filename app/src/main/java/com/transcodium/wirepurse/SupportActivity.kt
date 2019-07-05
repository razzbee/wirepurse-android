package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_support.*


class SupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }

        supportEmail.text = SUPPORT_EMAIL

        telegramCard.setOnClickListener {
            openLinkExternally(TELEGRAM_URL)
        }

        livechatCard.setOnClickListener {
            openLinkExternally(LIVECHAT_URL)
        }

        supportEmailCard.setOnClickListener {
            openLinkExternally(SUPPORT_EMAIL)
        }

        twitterCard.setOnClickListener {
            openLinkExternally(TWITTER_URL)
        }
    }
}
