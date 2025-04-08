package com.eazydelivery.app.ui.debug

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * Pager adapter for the monitoring dashboard
 */
class MonitoringPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    
    private val fragments = mutableListOf<Fragment>()
    private val fragmentTitles = mutableListOf<String>()
    
    /**
     * Add a fragment to the adapter
     * 
     * @param fragment The fragment to add
     * @param title The title of the fragment
     */
    fun addFragment(fragment: Fragment, title: String) {
        fragments.add(fragment)
        fragmentTitles.add(title)
    }
    
    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }
    
    override fun getCount(): Int {
        return fragments.size
    }
    
    override fun getPageTitle(position: Int): CharSequence {
        return fragmentTitles[position]
    }
}
