package com.transcodium.wirepurse

import android.app.Activity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView


fun handleBottomNav(
        mActivity: Activity,
        currentId: Int,
        navView: BottomNavigationView
){

    //set current view as checked
    navView.menu.findItem(currentId).isChecked = true

    navView.setOnNavigationItemSelectedListener {menuItem ->

        println("Biggies CLicked")
        when(menuItem.itemId){
            R.id.wallet -> {
                mActivity.startClassActivity(HomeActivity::class.java)
            }

            R.id.exchange -> {

                val data = Bundle().apply{
                    putString("title",mActivity.getString(R.string.exchange))
                }

                mActivity.startClassActivity(
                    activityClass = ComingSoonActivity::class.java,
                    data = data
                )
            }
        }

        return@setOnNavigationItemSelectedListener true
    }

    navView.setOnNavigationItemReselectedListener {
        return@setOnNavigationItemReselectedListener
    }
}

fun setActiveNav(
        currentId: Int,
        navView: BottomNavigationView
){
    //set current view as checked
    navView.menu.findItem(currentId).isChecked = true

}