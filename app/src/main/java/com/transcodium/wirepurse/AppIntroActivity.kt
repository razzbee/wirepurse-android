package com.transcodium.wirepurse

import android.os.Build
import android.os.Bundle
import android.transition.Explode
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import com.google.firebase.analytics.FirebaseAnalytics
import com.transcodium.wirepurse.classes.AppIntroData
import java.lang.Exception


class AppIntroActivity : AppIntro() {

    val fAnalytics by lazy{
        FirebaseAnalytics.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.setTheme(R.style.AppIntroTheme)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            with(window){
                requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
                exitTransition = Explode()
                enterTransition = Explode()
            }
        }

        val slidesData = listOf(

                AppIntroData(
                        R.string.cutting_edge_sucurity,
                        R.string.builtin_security_desc,
                        R.drawable.ic_privacy,
                        R.color.lightBlue600
                ),

                AppIntroData(
                  title = R.string.multi_coin_wallet,
                  desc  = R.string.multi_coin_wallet_desc,
                  image = R.drawable.ic_mobile_wallet,
                  bgColor =  R.color.appIntroSliderOrange
                ),

                AppIntroData(
                    title = R.string.builtin_exchange,
                    desc  = R.string.builtin_exchange_desc,
                    image = R.drawable.ic_builtin_exchange,
                    bgColor =  R.color.amber700
                ),


                AppIntroData(
                    title = R.string.extra_premium_service,
                    desc  = R.string.extra_premium_service_desc,
                    image = R.drawable.ic_store,
                    bgColor =  R.color.pinkA400
                )
        )//end list

        //hide title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

          setDepthAnimation()
        //setFlowAnimation()

        val textColor = ContextCompat.getColor(this,R.color.whiteAlpha70)


        /**
         * loop and display slide
         */
        for(slideObj in slidesData){

            try {
                val sliderPage = SliderPage()
                sliderPage.title = getString(slideObj.title)
                sliderPage.description = getString(slideObj.desc)
                sliderPage.imageDrawable = slideObj.image
                sliderPage.bgColor = ContextCompat.getColor(this, slideObj.bgColor)


                sliderPage.titleColor = textColor
                sliderPage.descColor = textColor

                addSlide(AppIntroFragment.newInstance(sliderPage))
            }catch (e: Exception){

                val msg = "${slideObj.title} - ${e.message}"

                val logData = Bundle().apply {
                    putString("APP_INTRO_CRASH",msg)
                }

                fAnalytics.logEvent("APP_INTRO_CRASH",logData)
                e.printStackTrace()
            }
        }//end for loop


    }//end oncreate

    /**
     * onDonePressed
     * @param currentFragment
     */
     override fun onDonePressed(currentFragment: Fragment){
        super.onDonePressed(currentFragment)

        //close the intro
        closeIntro()
     }//end fun

    /**
     * onBackPressed
     */
     override fun onBackPressed(){
        //minimize the app
        minimizeApp()
     }//end on back pressed


    /**
     * onSkipPressed
     * @currentFragment
     */
    override  fun  onSkipPressed(currentFargment: Fragment){
        super.onSkipPressed(currentFargment)

        //close the intro
        closeIntro()
    }//end onskipPressed


    override fun onResume() {

        val introCompleted = sharedPref().getBoolean("intro_completed",false)

        //if the intro has been completed already dont show
        //just skip to next activity
        if(introCompleted){
            closeIntro(false)
        }

        //if we are here then it means, the intro was not completed
        super.onResume()

        //lets hide status bar
        hideStatusBar()

        //hide action bar
        this.supportActionBar?.hide()

    }//end on resume


    /**
     * openLoginActivity
     */
    private fun closeIntro(updateDB: Boolean = true){

        if(updateDB) {
            //lets update status that user has finished intro
            sharedPref().edit{
                putBoolean("intro_completed", true)
            }//end edit
        }

        //open activity AuthActivity
        startClassActivity(LoginActivity::class.java)

        //finish the activity
        this.finish()
    }//end open login activity


}//end activity class
