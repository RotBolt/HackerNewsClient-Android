package io.dagger.hackernews.ui.home.banner

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.dagger.hackernews.data.model.Item

class BannerAdapter(fm: FragmentManager, private val items: List<Item>) : FragmentStatePagerAdapter(fm) {

    private val bannerItems = Array(items.size){
        TopBannerFragment.newInstance(items[it])
    }
    override fun getItem(position: Int): Fragment {
        return bannerItems[position % bannerItems.size]
    }
    override fun getCount() = Int.MAX_VALUE
}