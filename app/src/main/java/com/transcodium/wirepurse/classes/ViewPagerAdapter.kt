package com.transcodium.wirepurse.classes

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(
         fm: FragmentManager
) : FragmentPagerAdapter(fm){

    private val fragmentList = mutableListOf<Fragment>()
    private val fragmentTitles = mutableListOf<String>()

    override fun getCount(): Int = fragmentList.size

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override  fun getPageTitle(position: Int): CharSequence? {
        super.getPageTitle(position)

        return fragmentTitles[position]
    }

    /**
     * addFragment
     */
    fun addFragment(frag: Fragment, title: String): ViewPagerAdapter{
        fragmentList.add(frag)
        fragmentTitles.add(title)

        return this
    }

}//end class