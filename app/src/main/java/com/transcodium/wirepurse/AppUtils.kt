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
#  Project Android
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 26/07/2018
 **/

package com.transcodium.wirepurse

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.firebase.jobdispatcher.*
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.jaeger.library.StatusBarUtil
import com.tapadoo.alerter.Alerter
import com.transcodium.wirepurse.classes.*
import com.transcodium.wirepurse.classes.Account
import com.transcodium.wirepurse.classes.AppAlert
import com.transcodium.wirepurse.classes.Status
import kotlinx.android.synthetic.main.drawer_app_bar.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.lang.NumberFormatException
import java.math.RoundingMode
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass


/**
 * Activity.sharedPref
 * @return SharedPreference
 */
fun Context.sharedPref() = getDefaultSharedPreferences(this)

/**
 * secureSharedPre
 */
fun Context.secureSharedPref() = SecureSharedPref(this)

/**
 * minmizeApp
 */
fun Activity.minimizeApp(){
    val i = Intent(Intent.ACTION_MAIN)
    i.addCategory(Intent.CATEGORY_HOME)
    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(i)
}//end minimize app


/**
 * hideStatusBar
 */
fun Activity.hideStatusBar(){

    //get decor view
    val decorView: View = window.decorView

    //fullscreen flag
    val uiOptions: Int = View.SYSTEM_UI_FLAG_FULLSCREEN

    //set ui visibility to full screen
    decorView.systemUiVisibility = uiOptions
}//end function


/**
 *
 */
/**
 *startNewActivity
 **/
fun <T> Activity.startClassActivity(
        activityClass: Class<T>,
        clearActivityStack: Boolean = false,
        data: Bundle? = null
){

    //leave this intent to auth intent
    val i = Intent(this,activityClass)

    //put extra data
    if(data != null) {
        i.putExtras(data)
    }

    //if clear activity Stack is true
    if(clearActivityStack) {
        i.flags = (
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK
          )
    }//end if

    startActivity(i)

}//end fun

/**
 * UTCDate
 * @return Calendar
 **/
fun UTCDate() = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

/**
 * isLoggedIn
 * @return Boolean
 **/
fun Context.isLoggedIn(): Boolean = Account(this).isLoggedIn()

/**
 *isValidEmail
 */
fun String.isValidEmail(): Boolean{
    if(this.isEmpty()){
        return false
    }

    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}//end

/**
 * getOrCreateDeviceId
 **/
fun getDeviceId(context: Context): String{

    val id = context.secureSharedPref().getString(DEVICE_ID,"")

    if(!id.isNullOrEmpty()){
       return  id
    }

    //lets generate a new one and save it
    val deviceId = UUID.randomUUID().toString()

    //lets save it
    context.secureSharedPref().put(DEVICE_ID, deviceId)

    return deviceId
}//end


/**
 *vibrate
 **/
fun Activity.vibrate(pattern: List<Long>? = listOf(0L,15L)){

    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if(!vibrator.hasVibrator()){
        return
    }

    //val vibrationE = VibrationEffect.
    vibrator.vibrate(pattern!!.toLongArray(),-1)
}



/**
 * FromBitsToByte
 **/
fun Int.fromBitToByte(): Int{
    return (this / java.lang.Byte.SIZE).toInt()
}

/**
 * convertToBitMetric
 **/
fun Int.toBitMetric(): Int{
    return (this * java.lang.Byte.SIZE)
}

/**
 *isMashmelloOrHigher
 **/
fun isMarshmallowOrHeigher() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M


/**
 *hideAlert
 **/
fun hideAlert(activity: Activity) {
    if(Alerter.isShowing){ Alerter.hide() }
}//end

/**
 * isNetworkAvailable
 **/
fun Context.isNetworkAvailable(): Boolean {

    val conManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetInfo = conManager.activeNetworkInfo

    return (activeNetInfo != null && activeNetInfo.isConnected)
}//end


 fun Int.darken(fraction: Double): Int {

    var color = this
    var red = Color.red(color)
    var green = Color.green(color)
    var blue = Color.blue(color)
    red = darkenColor(red, fraction)
    green = darkenColor(green, fraction)
    blue = darkenColor(blue, fraction)
    val alpha = Color.alpha(color)

    return Color.argb(alpha, red, green, blue)
}

 private fun darkenColor(color: Int, fraction: Double): Int {
    return Math.max(color - color * fraction, 0.0).toInt()
 }

 fun Int.lighten(fraction: Double): Int {

    var color = this

    var red = Color.red(color)
    var green = Color.green(color)
    var blue = Color.blue(color)
    red = lightenColor(red, fraction)
    green = lightenColor(green, fraction)
    blue = lightenColor(blue, fraction)
    val alpha = Color.alpha(color)
    return Color.argb(alpha, red, green, blue)
}

    private fun lightenColor(color: Int, fraction: Double): Int {
        return Math.min(color + color * fraction, 255.0).toInt()
    }


/**
 * calculateColumns
 **/
fun Activity.calColumns(minWidth: Int): Int {

    val displayMetrics = resources.displayMetrics
    val dpWidth = displayMetrics.widthPixels / displayMetrics.density
    return (dpWidth / minWidth).toInt()
}


/**
 * rotate
 */
fun Bitmap.rotate(degree: Float, pivotX: Float, pivotY: Float): Bitmap{

    val m = Matrix().apply {
        postRotate(degree,pivotX,pivotY)
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}


/**
 * updateHomeUICoinName
 */
fun Activity.setToolbarTitle(
        titleText: String
){

    val txtView = topToolbarTitle
        txtView.animate()
                .translationY(txtView.height.toFloat())
                .alpha(0f)
                .setDuration(500)
                .withEndAction {

                    txtView.text = titleText

                    txtView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(500)
                            .start()

                }.start()
}//end


/**
 * setStatusBarColor
 **/
fun Activity.setStatusBarColor(color: Int){

    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
        StatusBarUtil.setColor(this,color)
    } else {
        window.statusBarColor = color
    }
}


suspend fun <T> awaitEvent(block: (h: com.transcodium.wirepurse.classes.Handler<T>) -> Unit) : T {
    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        try {
            block.invoke(com.transcodium.wirepurse.classes.Handler { t ->
                cont.resume(t)
            })
        } catch(e: Exception) {
            cont.resumeWithException(e)
        }
    }
}

/**
 * handleAppError
 **/
fun AppErrorUI(
        activity: Activity,
        status: Status,
        showAlert: Boolean? = true,
        killOnSevere: Boolean? = true
){

    if(showAlert!!){
        AppAlert(activity).showStatus(status)
    }

    if(killOnSevere!! && status.isSevere()){
        Account(activity).doLogout(status)
    }
}//end fun


/**
 *launchIO
 */
fun launchIO(block : suspend CoroutineScope.() -> Unit) : Job{

    val scope = CoroutineScope(Dispatchers.IO)

    return  scope.launch{
            block.invoke(this)
    }
}

/**
 * launch io
 */
fun launchUI(block : suspend CoroutineScope.() -> Unit) : Job{

    val scope = CoroutineScope(Dispatchers.Main)

    return  scope.launch{
        block.invoke(this)
    }
}


/**
 * startTask
 **/
fun <T : JobService> startPeriodicJob(
        mContext: Context,
        tag: String,
        clazz: KClass<T>,
        triggerInterval: Pair<Int,Int>,
        lifeTime: String? = "until_next_boot"
): com.firebase.jobdispatcher.Job {

    val jobLifeTime = if(lifeTime == "forever"){
        Lifetime.FOREVER
    } else {
        Lifetime.UNTIL_NEXT_BOOT
    }

    val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(mContext))

    val job = dispatcher.newJobBuilder()
            .setService(clazz.java)
            .setTag(tag)
            .setLifetime(jobLifeTime)
            .setTrigger(Trigger.executionWindow(
                    triggerInterval.first,
                    triggerInterval.second)
            )
            .setRecurring(true)
            .setReplaceCurrent(true)
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
            .build()

    dispatcher.mustSchedule(job)

    return job
}//end fun

/**
 * setPeriodic
 **/
fun setPeriodic(interval: Long, block: TimerTask.()->Unit): Timer{

    val timer = Timer()

    timer.scheduleAtFixedRate(object : TimerTask(){
        override fun run() {
            block.invoke(this)
        }
    },0L,interval)

    return timer
}//end fun

/**
 * toMD5
 */
fun String.toMD5(): String? {
    return hashData(this,"MD5")
}

/**
 * sha256
 */
fun String.toSha256(): String? {
    return hashData(this,"SHA-256")
}


/**
 * hashData
 */
fun hashData(str: String, algo: String): String? {

    return try{

        val md = MessageDigest.getInstance(algo)

        val digest = md.digest(str.toByteArray())

        val result = StringBuilder()

        for (byte in digest){
            result.append(String.format("%02x", byte))
        }

         result.toString()

    }catch (e: NoSuchAlgorithmException){
        Log.e("toMD5","MD5 Algorithm not found")
        e.printStackTrace()

        null
    }
}

/**
 * getGravatarUrl
 **/
fun getGravatar(userEmail: String): String {

    val emailMd5 = userEmail.toMD5()

    return "$GRAVATAR_URL/$emailMd5?s=160&r=g&d=$GRAVATAR_FALLBACK"
}


fun toDip(c: Context, pixel: Float): Float {
    val density = c.resources.displayMetrics.density
    return pixel / density
}


/**
 * IOCoroutine
 **/
val IO = CoroutineScope(Dispatchers.IO)

val UI = CoroutineScope(Dispatchers.Main)


/**
 * generateQRCode
 **/
fun generateQRCode(
        data: String,
        size: Int = 200,
        imgView: ImageView? = null
): Status{

    val multiFormatWriter = MultiFormatWriter()

    return try{

        val bitMatrix = multiFormatWriter.encode(data,BarcodeFormat.QR_CODE,size,size)

        val qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (x in 0 until size) {
            for (y in 0 until size) {
                qrCodeBitmap.setPixel(x, y, if (bitMatrix.get(x, y))
                    Color.BLACK
                else
                    Color.TRANSPARENT)
            }
        }

        //show image barcode
        imgView?.setImageBitmap(qrCodeBitmap)

         successStatus(data = qrCodeBitmap)
    }catch(e: Exception){

        Log.e("QRCODE_ERROR","Failed to generate QR code")
        e.printStackTrace()

        errorStatus()
    }

}//end generate qr code


/**
 * showContentLoader
 **/
 fun ProgressBar.show() = UI.launch{ setVisibility(View.VISIBLE) }

 fun ProgressBar.hide() = UI.launch{ setVisibility(View.GONE) }

 fun Context.hasCamera(): Boolean =  packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)


/**
 * Intent.putStatus
 **/
fun Intent.setStatus(data: Status): Intent {
     this.data = Uri.parse(data.toJsonString())
    return this
}//end fun

/**
 * Intent.getStatus
 **/
fun Intent.getStatus(): Status? {
    val statusStr = dataString ?: return errorStatus()
    return statusStr.toStatus()
}//end fun


fun Activity.dialog(block: AlertDialog.Builder.() -> AlertDialog.Builder){
    val d = AlertDialog.Builder(this,R.style.Theme_AppCompat_Light_Dialog)

    val a = block(d).create()

    a.show()

    val negativeBtn = a.getButton(DialogInterface.BUTTON_NEGATIVE)

    negativeBtn.setBackgroundColor(Color.TRANSPARENT)

    negativeBtn
            .setTextColor(getColorRes(R.color.colorAccent))

    val positionBtn = a.getButton(DialogInterface.BUTTON_POSITIVE)

    positionBtn.setBackgroundColor(Color.TRANSPARENT)

    positionBtn
            .setTextColor(getColorRes(R.color.colorPrimary))
}//end class


/**
 * colorId
 */
fun Activity.getColorRes(colorId: Int) : Int{
 return ContextCompat.getColor(this,colorId)
}


/**
 * startInAppAuth
 **/
fun Activity.startInAppAuth(){

    val i = Intent(this,PinCodeAuthActivity::class.java)

    //if user is logged in, lets get check for inApp Auth
    startActivityForResult(i, INAPP_AUTH_REQUEST_CODE)
}//end fun


/**
 * doubleToPlainString
 **/
fun doubleToString(data: Double): String {
    return data.toBigDecimal().stripTrailingZeros().toPlainString()
}//end

/**
 * round
 **/
fun Double.round(
        decimalPoint: Int,
        roundMode: RoundingMode? = RoundingMode.HALF_EVEN
): Double {
    return this.toBigDecimal()
            .setScale(decimalPoint,roundMode!!)
            .stripTrailingZeros()
            .toDouble()
}//end fun

fun Activity.getWindowSize(): DisplayMetrics{
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metrics)

    return metrics
}


/**
 * getUserInfo
 */
fun Context.getUserInfo(): JSONObject? {

    val mContext = this

    if(!mContext.isLoggedIn()){

        if(mContext is Activity) {
            Account(mContext).doLogout(errorStatus(R.string.kindly_login_to_continue))
            mContext.finish()
        }

        return  null
    }

    val userInfo = secureSharedPref()
            .getJSONObject(USER_AUTH_INFO)

    if(userInfo == null){

        mContext.fireLog("fetch_user_error","Failed to retrieve user info")

        if(mContext is Activity) {
            Account(mContext).doLogout(errorStatus(R.string.kindly_login_to_continue))
        }

        return null
    }

    return userInfo
} //en fun


/**
 * isPinCodeEnabled
 **/
fun Context.isPinCodeEnabled(): Boolean {
    val sharedPref = sharedPref()
    return sharedPref.getBoolean("fingerprint_enabled",false) ||
           sharedPref.getString("pin_code",null) != null
}

/**
 * openLinkExternally
 **/
fun Activity.openLinkExternally(link: String){

    try {
        if (link.isValidEmail()) {

            val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", link, null))

            startActivity(
                    Intent.createChooser(intent, "")
            )
        } else {

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
    } catch (e: java.lang.Exception){
        e.printStackTrace()
    }
}//end fun


/**
 * epochSecondsToTime
 **/
fun epochMillisToTime(context: Context,timeMillis: Long): String {


    val numFormat = NumberFormat.getInstance()

    val timeSeconds = (System.currentTimeMillis() - timeMillis) / 1000

        var time = ""

        if(timeSeconds <= 1){
            val secLocal = numFormat.format(timeSeconds)
            time = "$secLocal ${context.getString(R.string.second_ago).capitalize()}"
        }
        else if(timeSeconds in 1..59){
            val secLocal = numFormat.format(timeSeconds)
            time = "$secLocal ${context.getString(R.string.seconds_ago).capitalize()}"
        }

        else if(timeSeconds == 60L){
            time =  context.getString(R.string.a_minute_a_go)
        }

        else if(timeSeconds in 60..86400){

            val formattedTime = SimpleDateFormat.getTimeInstance()
                    .format(Date(timeMillis))

            time = "${context.getString(R.string.today_at).capitalize()} $formattedTime"

        } else {

            time = SimpleDateFormat.getDateTimeInstance()
                    .format(Date(timeMillis))
        }

        return time
} //end fun


/**
 * epocMillisToDateTime
 */
fun Long.epochMillisUTCToLocalDateTime(): String {
   return SimpleDateFormat.getDateTimeInstance()
            .format(Date(this))
}//end fun


fun Context.hasNetworkConnection():Boolean {
        val cm =  getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null
}


fun SwipeRefreshLayout.setLoaderColors(activity: Activity){
    this.setColorSchemeColors(
            activity.getColorRes(R.color.colorPrimary),
            activity.getColorRes(R.color.colorAccent),
            activity.getColorRes(R.color.green)
    )
}


/**
 * disable softIn
 */
fun EditText.disableSoftKeyboard(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        showSoftInputOnFocus = false
    } else {
        setTextIsSelectable(true)
    }
} //end


fun View.hide() { this.visibility = View.GONE }

fun View.show() { this.visibility = View.VISIBLE }


fun confirmExitDialog(activity: Activity){

    AlertDialog.Builder(activity)
        .setCancelable(false)
        .setMessage(R.string.confirm_exit_app)
        .setNegativeButton(R.string.cancel) { d, v ->
            d.dismiss()
        }
        .setPositiveButton(R.string.yes) { d, v ->
            activity.finishAffinity()
        }
        .show()
}


fun Context.fireLog(name: String,msg: String,extraParams: Bundle? = null){

    Log.e(name,msg)

    val firebaseAnalytics = FirebaseAnalytics.getInstance(this)

    val data = Bundle().apply {
        putString(name,msg)

        if(extraParams != null){
            putAll(extraParams)
        }
    }

    firebaseAnalytics.logEvent(name,data)
}


fun JSONObject.merge(data: JSONObject): JSONObject{
    for(key in data.keys()){
        this.put(key,data.get(key))
    }

    return this
}

fun Activity.openInfoActivity(status: Status){

    val data = Bundle().apply {
        putString("status",status.toJsonString())
    }

    startClassActivity(InfoActivity::class.java,true,data)
}


/**
 * copyToClipboard
 **/
fun Context.copyToClipboard(text: String){
     val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
     val clip = ClipData.newPlainText(null, text)
     clipboard.primaryClip = clip
}//end fun


fun String.isInt(): Boolean {

    if(this.trim().isEmpty()){
        return false
    }

    return try{
        this.toInt()
        true
    } catch (e: NumberFormatException){
        false
    }
}

