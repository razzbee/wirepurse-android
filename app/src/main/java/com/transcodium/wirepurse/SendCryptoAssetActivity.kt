package com.transcodium.wirepurse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.transcodium.wirepurse.classes.*

import kotlinx.android.synthetic.main.activity_send_crypto_asset.*
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.coroutines.launch
import org.json.JSONObject


class SendCryptoAssetActivity : ActivityDialogBase() {


    var cryptoSymbol: String? = null

    var assetInfo: JSONObject? = null

    val mActivity by lazy { this }

    var hasPaymentId : Boolean = false

    val viewPager by lazy {
        findViewById<ViewPager>(R.id.viewPager)
    }

    val walletCore = WalletCore()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_crypto_asset)


        IO.launch {

            val data = intent.extras!!

            cryptoSymbol = data.getString("asset_symbol") ?: null

            if (cryptoSymbol == null) {
                opError(errorStatus(R.string.unknown_asset))
                return@launch
            }

            val assetInfoStatus = walletCore.getAssetInfo(mActivity, cryptoSymbol!!)

            if (assetInfoStatus.isError()) {
                opError(assetInfoStatus); return@launch
            }

            assetInfo = assetInfoStatus.getData<JSONObject>()

            if (assetInfo == null) {
                opError(); return@launch
            }


            dialogTitle.text = mActivity.getString(R.string.send_space_asset, cryptoSymbol!!.toUpperCase())

            hasPaymentId = assetInfo!!.optBoolean("has_payment_id",false)

            UI.launch {

                if(hasPaymentId){
                    changeViewPagerHeight()
                }

                //close dialog
                closeModal.setOnClickListener { mActivity.finish() }

                val adapter = ViewPagerAdapter(supportFragmentManager)


                adapter.addFragment(
                        SendCryptoAssetFragment.newInstance(
                                layoutId = R.layout.send_crypto_asset_external,
                                assetInfo = assetInfo!!
                        ),
                        mActivity.getString(R.string.send_to_address)
                )


                adapter.addFragment(

                        SendCryptoAssetFragment.newInstance(
                                layoutId = R.layout.send_crypto_asset_internal,
                                assetInfo = assetInfo!!
                        ),

                        mActivity.getString(R.string.send_to_user)
                )


               viewPager.adapter = adapter


                tabLayout.setupWithViewPager(viewPager)

            }//end ui operations

        }//end IO operation


    }//end onCreate


    /**
     * changeViewPagerHeight
     */
    fun changeViewPagerHeight(type: String? = "increase") {


        val noToAdd = if (type == "increase") {
            180
        } else {
            -180
        }

        val lp = viewPager.layoutParams

        val newHeight = lp.height + noToAdd

        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = newHeight

        viewPager.requestLayout()

        Log.e("NEW_H", viewPager.layoutParams.height.toString())
     }//end fun


    /**
     * forward all onActivityResults to fragments
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val allFragments = supportFragmentManager.fragments

        //send to fragments
        for(fragment in allFragments) {
          fragment.onActivityResult(requestCode, resultCode, data)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * forward all onRequestPermissionsResult to fragments
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        val allFragments = supportFragmentManager.fragments

        for(fragment in allFragments) {
           fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
       }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}//end class
