package io.dagger.hackernews.ui.home.banner

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.dagger.hackernews.data.model.Item

class BannerAdapter(fm: FragmentManager, private val items: List<Item>) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return TopBannerFragment.newInstance(items[position])
    }
    override fun getCount() = items.size
}