package com.transcodium.wirepurse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
//import com.facebook.stetho.Stetho
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.common.api.GoogleApiClient
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.transcodium.wirepurse.classes.*
import org.json.JSONObject
import java.lang.Exception

class LoginActivity : RootActivity() {


    //we will use intent key to detect google signin result
    private val GOOGLE_SIGNIN = 12

    //we wil use boolean for the other login results
    private var IS_FACEBOOK_SIGNIN = false
    private var IS_TWITTER_SIGNIN = false


    val activity by lazy {
        this
    }

    val appAlert by lazy{
        AppAlert(activity)
    }

    //facebook callback manager
    private val fbCallbackManager by lazy{
        CallbackManager.Factory.create()
    }

    //twitter Client
    private val twitterAuthClient by lazy {
        TwitterAuthClient()
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val bundle = intent?.extras

        val statusStr: String? = bundle?.getString("status")

        if(statusStr != null){
            val statusObj = statusStr.toStatus()
            AppAlert(this).showStatus(statusObj)
        }else{
            Log.e("Status",statusStr.toString())
        }

        loginWithEmail.setOnClickListener{startClassActivity(EmailLoginActivity::class.java)}


        loginWithGoogle.setOnClickListener{ signInGoogle() }

        loginWithFacebook.setOnClickListener{ signInFacebook() }

        loginWithTwitter.setOnClickListener{ signinTwitter() }

    }//end fun onCreate

    //onback press
    override fun onBackPressed() {
        confirmExitDialog(this)
    }

    /**
     * signInGoogle
     */

    private fun signInGoogle() = UI.launch{

        //init google sigin
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(GOOGLE_WEBCLIENT_ID)
                    .requestEmail()
                    .build()

        val mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = mGoogleSignInClient.signInIntent

        //start activit
        startActivityForResult(signInIntent, GOOGLE_SIGNIN)

    }//end request google login

    /**
     * signInFacebook
     */
    private fun signInFacebook() {

        //set IS_FACEBOOK_SIGNIN to true
        IS_FACEBOOK_SIGNIN = true

        //get instance of the login manager
        val loginManager = LoginManager.getInstance()

        //lets set the permissions or scopes
        loginManager.logInWithReadPermissions(this,listOf("email","public_profile"))

        //lets now start the auth and listen to the call back
        loginManager.registerCallback(
                fbCallbackManager,
                object: FacebookCallback<LoginResult> {

            //listen to success callback
            override fun onSuccess(result: LoginResult) {

                //access token
                val accessToken = result.accessToken

                launch(Dispatchers.IO) {
                    SocialLoginCore(activity)
                            .processSocialLogin("facebook",accessToken)
                }

            }//end on success

            //listen to error
            override fun onError(error: FacebookException) {

                //show auth failed
                AppAlert(activity).error(R.string.facebook_login_failed)

                Log.e("Facebook Auth Error: ",error.message)

                error.printStackTrace()
            }//end error

            //if cancelled
            override fun onCancel() {
                AppAlert(activity).error(R.string.facebook_login_cancelled)
            }//end

        })//end callback listener

    }//end sigin to facebook

    //sigin twitter
    fun signinTwitter(){

        //set signal to true
        IS_TWITTER_SIGNIN = true


        //init twitter
        val config = TwitterConfig.Builder(this)
                .logger(DefaultLogger(Log.ERROR))
                .twitterAuthConfig(TwitterAuthConfig(
                        TWITTER_CONSUMER_KEY,
                        TWITTER_CONSUMER_SECRET
                ))
                .debug(BuildConfig.DEBUG)
                .build()
        Twitter.initialize(config)

        twitterAuthClient.cancelAuthorize()


        //authorize request
        twitterAuthClient.authorize(this, object: Callback<TwitterSession>(){

            //on success
            override fun success(result: Result<TwitterSession>) {

                //session
                val session = result.data

                IO.launch {
                    SocialLoginCore(activity)
                            .processSocialLogin("twitter",session)
                }

            }//end success


            //if failed
            override fun failure(exception: TwitterException) {

                //auth failed
                appAlert.error(R.string.twitter_login_failed)

                //print stacktrace
                exception.printStackTrace()

                Log.e("Twitter Auth Failed:",exception.message)

                SocialLoginCore(activity).logoutTwitterAccountAsync()
            }//end if failed

        })//end autheorize

    }//end sign in twitter



    /*
    * handle GoogleSingin Result
    */
    private fun handleGoogleSignInResult(data: Intent?){


        try{
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            val acct = task.getResult(ApiException::class.java)

            launch(Dispatchers.IO) {
                SocialLoginCore(activity).processSocialLogin("google",(acct as Any))
            }

        }catch(e: ApiException){

            AppAlert(activity).error(R.string.google_login_failed)

            SocialLoginCore(activity).logoutGoogleAccountAsync()

            Log.e("Google Auth Failed: ","${e.statusCode} - ${e.message}")
            e.printStackTrace()
        }
    }//end handle google signin result


    /**
     * onActivityResult
     */
    override  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        //if its pin code activity auth
        if(requestCode == INAPP_AUTH_REQUEST_CODE && resultCode == Activity.RESULT_OK){

            val status = data?.getStatus()!!

            //if success, process
            if(status.isSuccess()){
                Account(activity).handlePostLogin(
                        activity = this,
                        checkTwoFa = true
                )
                return
            } //end if
        }

        //if the returned results is from google signin request
        else if(requestCode == GOOGLE_SIGNIN){
            handleGoogleSignInResult(data)
        }
        //if is facebook
        else if(IS_FACEBOOK_SIGNIN){

            //let fb handle the result
            fbCallbackManager.onActivityResult(requestCode,resultCode,data)
        }

        //if twitter
        else if(IS_TWITTER_SIGNIN) {

           twitterAuthClient.onActivityResult(requestCode,resultCode,data)

        }//end if twitter

        super.onActivityResult(requestCode, resultCode, data)
    }///end event fun


}//end class
