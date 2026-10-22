package com.deviceinfo.auto

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DeviceInfoTabPagerAdapter(
    activity: FragmentActivity,
    val sectionKeys: List<String>,
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = sectionKeys.size

    override fun createFragment(position: Int): Fragment =
        DeviceInfoTabFragment.newInstance(sectionKeys[position])
}
