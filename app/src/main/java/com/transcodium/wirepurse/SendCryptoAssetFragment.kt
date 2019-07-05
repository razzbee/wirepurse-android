package com.transcodium.wirepurse


import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import com.transcodium.wirepurse.classes.*
import com.xw.repo.BubbleSeekBar
import kotlinx.android.synthetic.main.activity_receive_crypto_asset.*
import kotlinx.android.synthetic.main.activity_send_crypto_asset.*
import kotlinx.android.synthetic.main.activity_webview.*
import kotlinx.android.synthetic.main.circular_progress_bar.*
import kotlinx.android.synthetic.main.send_crypto_asset_external.*
import kotlinx.android.synthetic.main.send_crypto_asset_external.testnet_warning
import kotlinx.android.synthetic.main.send_crypto_asset_external.view.*
import kotlinx.android.synthetic.main.send_crypto_asset_internal.*
import kotlinx.android.synthetic.main.send_crypto_asset_internal.view.*
import kotlinx.android.synthetic.main.verification_code_layout.*
import kotlinx.android.synthetic.main.verification_code_layout.view.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONObject

import java.lang.Exception
import java.math.RoundingMode
import java.net.URL


private const val LAYOUT_ID_PARAM = "layout_id"
private const val ASSET_INFO_PARAM = "asset_symbol"

private const val amountInputTextDecimalPoint = 10

class SendCryptoAssetFragment : Fragment() {

    private var assetInfo: JSONObject? = null
    private var assetSymbol: String? = null
    private var assetId: String? = null
    private var assetChain: String? = null
    private var layoutId: Int? = null
    private var hasPaymentId: Boolean = false
    private  val APP_CAMERA_PERMISSION = 15
    private var transferMode: String? = null
    var mBubbleSlider: BubbleSeekBar? = null
    val mProgress by lazy{ Progress(mActivity!!) }
    var userTotalBalance: Double ? = null
    var withdrawalFee: Double? = null

    private var networkType = ""

    private var minWithdrawalAmount: Double? = null

    val walletCore = WalletCore()

    //dataPair prepared for sending
    var proccessedDataToSend : MutableList<Pair<String,Any>>? = mutableListOf()


    val mActivity by lazy{
        this.activity
    }


    val barCodeScanner by lazy {
        IntentIntegrator.forSupportFragment(this)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setOrientationLocked(true)
                .setBeepEnabled(true)
                .setPrompt(getString(R.string.scanner_toggle_torch))
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        arguments?.let {

            layoutId = it.getInt(LAYOUT_ID_PARAM)

            assetInfo = JSONObject(it.getString(ASSET_INFO_PARAM))

            assetSymbol = assetInfo!!.getString("symbol")

            assetId = assetInfo!!.getString("_id")

            assetChain = assetInfo!!.getString("chain")

            hasPaymentId = assetInfo!!.optBoolean("has_payment_id",false)

            minWithdrawalAmount = assetInfo!!.optDouble("minimum_withdrawal",0.0)
        }


        transferMode = if(layoutId == R.layout.send_crypto_asset_external){
            "external"
        } else {
            "internal"
        }


        userTotalBalance = assetInfo!!.getDouble("balance")

        withdrawalFee = assetInfo!!.getDouble("withdrawal_fee")

        networkType = assetInfo!!.optString("network_type","")

        //listen to viewPage changes and clear the processedDataToSend
        //to avoid multiple requests
        mActivity?.viewPager?.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{

            override fun onPageScrollStateChanged(state: Int) {
                proccessedDataToSend = null
            }

            override fun onPageSelected(position: Int) {
                proccessedDataToSend = null
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })//end page  changes

        
    }//end onCreate

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val rootView =  inflater.inflate(
                layoutId!!,
                container,
                false
        )

        //hide scan message if no camera
        if(!mActivity!!.hasCamera()){
            mActivity!!.scanWithCamera?.visibility = View.GONE
        }

        rootView.scanWithCamera?.setOnClickListener { openQRCodeScanner() }

        val chainName = walletCore.getChainName(assetChain!!).capitalize()

        val chainNameAndSymbol = "$chainName (${assetChain?.toUpperCase()})"

        if(!networkType.equals("mainnet",true)){
            rootView?.testnet_warning?.show()
        } else {
            rootView?.testnet_warning?.hide()
        }


        //external address input hint
        rootView?.externalAddressToSendInputLayout?.hint = getString(
                R.string.asset_name_space_address,chainNameAndSymbol
        )

        //if chain has payment id or destination tag requirement, lets make the input visible
        if(hasPaymentId){
            rootView?.externalpaymentIdInputInputLayout?.visibility = View.VISIBLE
        }


        //process send External
        rootView?.externalSendBtn?.setOnClickListener { processSendExternal() }

        rootView?.internalSendBtn?.setOnClickListener { processSendInternal() }


        val userBalance = doubleToString(
                assetInfo!!.optDouble("balance",0.0)
        )

        //show user balance
        rootView.findViewById<TextView>(R.id.userBalance)
                .text = ("$userBalance ${assetSymbol!!.toUpperCase()}")

        processTxSummary(rootView,transferMode!!)


        //process slider events
        processSlider(rootView,transferMode!!)


        rootView?.requestCodeBtn?.setOnClickListener {
            IO.launch {

                VerificationCode(mActivity!!).sendCode(
                        activityGroup = "withdrawal",
                        hasAuth = true
                )
            }
        } //end


        return rootView
    }//end fun


    /**
     * process slider
     */
    fun processSlider(rootView: View, transferMode: String){

        //get slider
        mBubbleSlider = rootView.findViewById(R.id.transferAmountSlider)

        val amountToSendInput = if(transferMode == "external"){
            rootView.findViewById<TextInputEditText>(R.id.externalAmountToSendInput)
        } else {
            rootView.findViewById(R.id.internalAmountToSendInput)
        }

        //user balance
        val userBalance = assetInfo?.getDouble("balance") ?: 0.0


        val decimalPoint = if(userBalance.toString().startsWith("0.")){
            amountInputTextDecimalPoint
        } else {
            3
        }

        /**
         * fix slider on scroll change
         */
        rootView.findViewById<NestedScrollView>(R.id.scrollView)
                ?.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                    mBubbleSlider?.correctOffsetWhenContainerOnScrolling()
         }//ens scroll correction

        mBubbleSlider?.onProgressChangedListener = (object: BubbleSeekBar.OnProgressChangedListener{

            override fun onProgressChanged(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {

                val amountToSend = (( progressFloat.toDouble() / 100 ) * userBalance)
                   .round(decimalPoint,RoundingMode.HALF_UP)

                amountToSendInput.setText(amountToSend.toString())
            }

            override fun getProgressOnFinally(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}

            override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float) {}
        })

    }//end fun


    /**
     * qrCodeScanner
     */
    private fun openQRCodeScanner() {

        //lets ceck if app has permssion to camera
       val camPerm = ContextCompat.checkSelfPermission(mActivity!!, Manifest.permission.CAMERA)

        //if we have camera permission, then scan
        if(camPerm == PackageManager.PERMISSION_GRANTED) {
            barCodeScanner.initiateScan()
        }else{

           val alert = AlertDialog.Builder(mActivity!!,R.style.Theme_AppCompat_Light_Dialog)
                        .setMessage(R.string.camera_permission_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok){dialog, _ ->

                            //request permission
                            ActivityCompat.requestPermissions(
                                    mActivity!!,
                                    arrayOf( Manifest.permission.CAMERA),
                                    APP_CAMERA_PERMISSION
                            )
            }

            alert.show()
        }
    }//end

    /**
     * processTxSummary
     */
    fun processTxSummary(rootView: View, transferMode: String){


        val assetSymbolUpperCase = assetSymbol?.toUpperCase()


        if(transferMode == "external"){


            if(withdrawalFee == null){
                rootView.visibility = View.INVISIBLE
                AppAlert(mActivity!!).error(R.string.unexpected_error)
                Log.e("APP_ERROR","Withdrawal Fee is null")
            }


            val txFeeStr = "${doubleToString(withdrawalFee!!)} $assetSymbolUpperCase"

            rootView.externalTxFee.text = txFeeStr

            rootView.externalTxTotalAmount.text = txFeeStr

            val externalAmountToSendInputLayout = rootView.externalAmountToSendInputLayout

            externalAmountToSendInputLayout.isErrorEnabled = true

            val externalAmountToSendInput = rootView.externalAmountToSendInput

            externalAmountToSendInput
              .addTextChangedListener(object: TextWatcher {

                  override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                  override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                  override fun afterTextChanged(s: Editable?) {

                      externalAmountToSendInputLayout.error = ""

                      if(s == null){
                          return
                      }

                      //get the decimal data
                      val deciamlData = s.toString().substringAfter(".")

                      var inputText = s.toString()

                      if(deciamlData.length > amountInputTextDecimalPoint){

                          inputText = s.substring(0,s.length-1)

                          //start holds the text length
                          externalAmountToSendInput.setText(inputText)
                          externalAmountToSendInput.setSelection(inputText.length)
                      }

                      val inputTextDouble = inputText.toDoubleOrNull()
                                ?: 0.0

                      //if less than min withdrawal amount
                      if(inputTextDouble < minWithdrawalAmount!!){
                          externalAmountToSendInputLayout.error = getString(
                                  R.string.min_withdrawal_amount_error,
                                  "${minWithdrawalAmount!!} $assetSymbolUpperCase"
                          )
                      }

                      val totalAmount = inputTextDouble.plus(withdrawalFee!!)
                                .round(amountInputTextDecimalPoint,RoundingMode.UP)


                      //if total amount is greater than balance
                      if(totalAmount > userTotalBalance!!){
                          externalAmountToSendInputLayout.error = getString(R.string.insufficient_balance)
                      }

                      val totalAmountStr = doubleToString(totalAmount)

                      //set the total amount
                      rootView.externalTxTotalAmount.text = ("$totalAmountStr $assetSymbolUpperCase")
                  }
              })


        } else { // if internal


            val txFee = 0.0

            val internalAmountToSendInputLayout = rootView.internalAmountToSendInputLayout

            internalAmountToSendInputLayout.isErrorEnabled = true

            val internalAmountToSendInput = rootView.internalAmountToSendInput

            rootView.internalTxFee.text = ("${doubleToString(txFee)} $assetSymbolUpperCase")

            internalAmountToSendInput
              .addTextChangedListener(object: TextWatcher{

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {

                    internalAmountToSendInputLayout.error = ""

                    if(s == null){
                        return
                    }

                    //get the decimal data
                    val deciamlData = s.toString().substringAfter(".")

                    var inputText = s.toString()

                    val inputTextNum = inputText.toDoubleOrNull() ?: 0.0

                    if(deciamlData.length > amountInputTextDecimalPoint){

                        inputText = s.substring(0,s.length-1)

                        //start holds the text length
                        internalAmountToSendInput.setText(inputText)
                        internalAmountToSendInput.setSelection(inputText.length)
                    }


                    //if total amount is greater than balance
                    if(inputTextNum > userTotalBalance!!){
                        internalAmountToSendInputLayout.error = getString(R.string.insufficient_balance)
                    }


                    rootView.internalTxTotalAmount.text = ("$inputText $assetSymbolUpperCase")
                }

             })
        }
    }//end fun


    /**
     * processSendInternal
     */
    fun processSendInternal(){

        proccessedDataToSend = null

        //lets proccess the data
        val recipientEmailAddress = internalReciepientEmailInput.text.toString()

        val amountToSend = internalAmountToSendInput.text.toString().toDoubleOrNull() ?: 0.0

        internalReciepientEmailInputLayout.error = ""

        internalAmountToSendInputLayout.error = ""

        var hasError = false

        if(!recipientEmailAddress.isValidEmail()){
            internalReciepientEmailInputLayout.error = getString(R.string.invalid_email_address)
            hasError = true
        }

        if(amountToSend <= 0){
            internalAmountToSendInputLayout.error = getString(R.string.invalid_amount_to_send)
            hasError = true
        }

        if(amountToSend > userTotalBalance!!){
            internalAmountToSendInputLayout.error = getString(R.string.insufficient_balance)
            hasError = true
        }

        if(hasError){ return }

        val totalAmount = amountToSend

        //lets show send confirmation
        showSendConfirmation(
                sendMode = "internal",
                address = recipientEmailAddress,
                amount = amountToSend,
                amountPlusFees = totalAmount
        )

    }//end fun



    /**
     * processSendExternal
     */
    fun processSendExternal(){

        proccessedDataToSend = null

        //lets proccess the data
        val externalAddress = externalAddressToSendInput.text.toString()

        val amountToSend = externalAmountToSendInput.text.toString().toDoubleOrNull() ?: 0.0

        externalAddressToSendInputLayout.error = ""

        externalAmountToSendInputLayout.error = ""

        var hasError = false

        if(!CryptoAddress.isValid(assetChain!!,externalAddress)){
            externalAddressToSendInputLayout.error = getString(R.string.invalid_crypto_address,assetChain?.toUpperCase())
            hasError = true
        }


        if(amountToSend <= 0.0){
            externalAmountToSendInputLayout.error = getString(R.string.invalid_amount_to_send)
            hasError = true
        }

        if(amountToSend > userTotalBalance!!){
            externalAmountToSendInputLayout.error = getString(R.string.insufficient_balance)
            hasError = true
        }


        if(hasError){ return }

        val paymentId: String? = externalpaymentIdInput.text?.toString()

        val totalAmount = (amountToSend + withdrawalFee!!)
                .round(amountInputTextDecimalPoint,RoundingMode.UP)


        //lets show send confirmation
        showSendConfirmation(
                sendMode = "external",
                address = externalAddress,
                amount = amountToSend,
                paymentId = paymentId,
                amountPlusFees = totalAmount
        )
    }//end fun


    /**
     * sendConfrimation
     */
    private fun showSendConfirmation(
            sendMode: String,
            address: String,
            amount: Double,
            amountPlusFees: Double,
            paymentId: String? = null
    ){

        //lets get verification code
        val verificationCodeLayout = verificationCodeInputLayout

        verificationCodeLayout.error = ""

        val verificationCode = verificationCodeInput.text.toString()

        if(verificationCode.isEmpty()){
            verificationCodeLayout.error = getString(R.string.verification_code_required)
            return
        }

        //clear data
        proccessedDataToSend = null

        var sendModeText = ""
        var fee = 0.0

        val assetSymbol = assetSymbol!!.toUpperCase()

        if(sendMode == "internal"){
            sendModeText = getString(R.string.internal)
        }else{
            sendModeText = getString(R.string.external)

            fee = assetInfo!!.getDouble("withdrawal_fee")
        }

        var dialogContent = """
            <div style="color:#455a64">
                <div>${getString(R.string.amount)}: $amount $assetSymbol</div>
                 <div>${getString(R.string.send_mode)}: ${sendModeText.capitalize()}</div>
                <div>${getString(R.string.recipient_address)}: $address</div>
                <div>${getString(R.string.withdrawal_fee)}: $fee $assetSymbol</div>
                <div>${getString(R.string.total_amount)}: $amountPlusFees $assetSymbol</div>
            </div>
        """.trimIndent()

        if(paymentId != null){
            dialogContent += "<div>${getString(R.string.payment_id_or_destination_tag)} : $paymentId</div>"
        }

        mActivity!!.dialog {
            setCancelable(false)
            setTitle(R.string.confirm_transfer)
            setMessage(Html.fromHtml(dialogContent))
            setNegativeButton(R.string.cancel){d,_-> d.cancel() }
            setPositiveButton(R.string.confirm){d,_->

                proccessedDataToSend =  mutableListOf()

                //lets process the data
                proccessedDataToSend!!.apply {
                        add(Pair("mode", sendMode))
                        add(Pair("amount", amount))
                        add(Pair("recipient_address", address))
                        add(Pair("payment_id", paymentId ?: ""))
                        //add(Pair("asset", assetSymbol!!))
                        add(Pair("verification_code",verificationCode))
                        add(Pair("crypto_id", assetId!!))
                }

                mActivity!!.startInAppAuth()
            }
        }

    }//end fun


    /**
     *  sendAssetTransferToServer()
     */
    fun  sendAssetTransferToServer() = IO.launch{

        //avoid duplicate requests
        //since viewPage changes listener clears data, any attempt to
        //send mutiple request will cause it to be ignored
        //clear proccessedDataToSend after request
        if(proccessedDataToSend == null || proccessedDataToSend!!.isEmpty()){
            return@launch
        }

        mProgress.show(
           bgColor = R.color.purpleDarken2,
           dismissable = false,
           blockUI = true
        )

        val sendStatus = TnsApi(mActivity!!)
                    .post(
                            requestPath = "wallet/withdraw",
                            params = proccessedDataToSend
                    )


        mProgress.hide()

        //clear proccessedDataToSend
        proccessedDataToSend = null

        AppAlert(mActivity!!).showStatus(sendStatus)

        if(sendStatus.isSuccess()){

            IO.async {
                NotificationCore().fetchAndProcessNotifications(mActivity!!,true)
                walletCore.networkFetchUserAssets(mActivity!!)
            }

            delay(3000)
            mActivity!!.finish()
        }
    }//end fun

    /**
     * parse And Proccess QR Data
     */
     fun processQRData(data: String){

        val requiredScheme = walletCore.getChainName(assetChain!!)

      //parse uri
       val uri = Uri.parse(data)

       val scheme = uri.scheme

       val queryString = uri.query

       var paymentAddress = data

       var label = ""

       var amount = 0.0


        if(scheme != null && requiredScheme != scheme){
            AppAlert(mActivity!!)
                    .error(
                            getString(R.string.invalid_qr_code,requiredScheme),
                     true
                    )
           return
       }else{
           paymentAddress =  paymentAddress.substringAfter(":")
       }

       //lets check query
        if(!queryString.isNullOrEmpty()){


          val queryStringSplit = queryString.split("&")

          for(keyValueStr in queryStringSplit) {

              val splitKeyValue = keyValueStr.split("=")

              if(splitKeyValue.size !=  2){ continue }

              val key = splitKeyValue[0]

              val value = splitKeyValue[1]

              when(key){
                  "amount" -> amount = value.toDoubleOrNull() ?: 0.0
                  //"label"  -> label = value
              }

          }//end for loop


          paymentAddress = paymentAddress.substringBefore("?")

        }//end if

        //fill form with data
        externalAddressToSendInput?.setText(paymentAddress)
        externalAmountToSendInput?.setText(amount.toString())

    }//end fun


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        //if pincode auth is successful
        if(requestCode == INAPP_AUTH_REQUEST_CODE && resultCode == RESULT_OK){
            sendAssetTransferToServer()
            return
        }//end if


        val barcodeResult = IntentIntegrator.parseActivityResult(requestCode,resultCode,data)

        if(barcodeResult != null){

            val barcodeContent = barcodeResult.contents

           if(barcodeContent == null) {
               mActivity?.toast(R.string.operation_aborted_by_user)
               return
           }


            processQRData(barcodeContent)

        } else {

            super.onActivityResult(requestCode, resultCode, data)
        }//end if
    }


    override  fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {

        when(requestCode){

            //if its Bar Code stuff
            APP_CAMERA_PERMISSION -> {

                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    barCodeScanner.initiateScan()
                }else{
                    mActivity!!.toast(R.string.camera_perm_denied)
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {

        @JvmStatic
        fun newInstance(
                layoutId: Int,
               assetInfo: JSONObject
        ) = SendCryptoAssetFragment().apply {

                arguments = Bundle().apply {
                    putInt(LAYOUT_ID_PARAM,layoutId)
                    putString(ASSET_INFO_PARAM, assetInfo.toString())
                }

        } //end apply

    }///end companion



}//end class
