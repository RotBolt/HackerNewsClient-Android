package io.dagger.hackernews.ui.home


import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import io.dagger.hackernews.R
import io.dagger.hackernews.data.model.Item
import io.dagger.hackernews.ui.ITEM_TYPE
import io.dagger.hackernews.ui.NewsItemAdapter
import io.dagger.hackernews.ui.home.banner.BannerAdapter
import io.dagger.hackernews.ui.home.banner.ZoomOutPageTransformer
import io.dagger.hackernews.utils.Errors
import io.dagger.hackernews.utils.isPortrait
import io.dagger.hackernews.utils.setTopDrawable
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_error.*
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.CoroutineContext

class HomeFragment : Fragment(), CoroutineScope {

    private val superVisor = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + superVisor

    private var autoChangeJob: Job? = null

    private var retrySnack: Snackbar? = null

    private var bannerIdx = 0

    private var isBannerLoaded = false
    private var isAutoChangeJobStarted = false

    private val dashViewModel by lazy {
        ViewModelProviders.of(this).get(DashNewsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bannerIdx = dashViewModel.bannerIdx

        loadContent()

        retrySnack = Snackbar.make(dashContainer, "You are offline", Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry") {
                refresh()
            }

        dashContainer.apply {
            setProgressViewOffset(true, 100, 250)
            setOnRefreshListener { refresh() }
        }
    }

    private fun loadContent() {

        launch {
            showStoryContainer()
            setBannerItems()
            setCategoryItems("New", false)
            setCategoryItems("Show", false)
            setCategoryItems("Job", false)
            setCategoryItems("Ask", false)
        }
    }

    private fun showOfflineView() {
        storiesContainer.isVisible = false
        tvError.apply {
            isVisible = true
            text = resources.getString(R.string.no_internet)
            val drawable = resources.getDrawable(R.drawable.ic_no_wifi)
            setTopDrawable(drawable)
        }
        dashContainer.isRefreshing = false
        launch {
            delay(300)
            retrySnack?.setText("You are offline")?.show()
        }
    }

    private fun showError() {
        storiesContainer.isVisible = false
        tvError.apply {
            isVisible = true
            text = "Error Occured"
            val drawable = resources.getDrawable(R.drawable.ic_interupt)
            setTopDrawable(drawable)
        }
        dashContainer.isRefreshing = false
        retrySnack?.setText("Error Occured")?.show()
    }

    private fun showStoryContainer() {
        tvError.isVisible = false
        storiesContainer.isVisible = true
        retrySnack?.dismiss()
    }

    private fun refresh() {
        launch {

            isAutoChangeJobStarted = false
            autoChangeJob?.cancel()
            isBannerLoaded = false
            bannerIdx = 0

            showStoryContainer()

            shimmerTop.apply {
                isVisible = true
                startShimmer()
            }
            shimmerShow.apply {
                isVisible = true
                startShimmer()
            }
            shimmerAsk.apply {
                isVisible = true
                startShimmer()
            }
            shimmerJob.apply {
                isVisible = true
                startShimmer()
            }
            shimmerNew.apply {
                isVisible = true
                startShimmer()
            }

            bannerPager.isVisible = false
            rvAsk.isVisible = false
            rvNew.isVisible = false
            rvJob.isVisible = false
            rvShow.isVisible = false

            setBannerItems(true)
            setCategoryItems("New", true)
            setCategoryItems("Show", true)
            setCategoryItems("Job", true)
            setCategoryItems("Ask", true)

            dashContainer.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBannerLoaded) {
            autoChangeJobStart()
        }
    }

    private fun autoChangeJobStart() {
        isAutoChangeJobStarted = true
        autoChangeJob = launch {
            while (true) {
                delay(3000)
                ++bannerIdx
                dashViewModel.bannerIdx = bannerIdx
                bannerPager.setCurrentItem(bannerIdx, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isAutoChangeJobStarted = false
        autoChangeJob?.cancel()
    }

    private fun setRecyclerView(rv: RecyclerView, list: List<Item>?, isRefresh: Boolean) {

        rv.apply {
            isVisible = true
            isNestedScrollingEnabled = false

            adapter = NewsItemAdapter(
                list ?: emptyList(),
                ITEM_TYPE.SMALL,
                requireActivity()
            )
            layoutManager = if (isPortrait(requireContext())) {
                if (!isRefresh) {
                    addItemDecoration(SpacesItemDecoration(40))
                }
                GridLayoutManager(requireContext(), 2)
            } else {
                GridLayoutManager(requireContext(), 4)
            }
        }
    }

    private suspend fun setCategoryItems(type: String, isRefresh: Boolean) {

        try {
            val items = dashViewModel.geItemsAsync(type, isRefresh).await()
            if (items.isNotEmpty()) {
                when (type) {
                    "New" -> {
                        shimmerNew.isVisible = false
                        shimmerNew.stopShimmer()
                        setRecyclerView(rvNew, items, isRefresh)
                    }
                    "Show" -> {
                        shimmerShow.isVisible = false
                        shimmerShow.stopShimmer()
                        setRecyclerView(rvShow, items, isRefresh)

                    }
                    "Job" -> {
                        shimmerJob.isVisible = false
                        shimmerJob.stopShimmer()
                        setRecyclerView(rvJob, items, isRefresh)

                    }
                    "Ask" -> {
                        shimmerAsk.isVisible = false
                        shimmerAsk.stopShimmer()
                        setRecyclerView(rvAsk, items, isRefresh)
                    }
                }
            }
        } catch (u: UnknownHostException) {
            showOfflineView()
        } catch (e: ConnectException) {
            showOfflineView()
        } catch (te: SocketTimeoutException) {
            showError()
        } catch (e: Errors) {
            when (e) {
                is Errors.OfflineException -> showOfflineView()
                is Errors.FetchException -> showError()
            }
        }
    }


    private suspend fun setBannerItems(isRefresh: Boolean = false) {
        try {
            val topItems = dashViewModel.geItemsAsync("Top", isRefresh).await()
            shimmerTop.apply {
                stopShimmer()
                isVisible = false
            }

            isBannerLoaded = true
            bannerPager.apply {
                isVisible = true
                adapter = BannerAdapter(requireFragmentManager(), topItems)
                setPageTransformer(true, ZoomOutPageTransformer())
                setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageSelected(position: Int) {
                        bannerIdx = position
                        dashViewModel.bannerIdx = bannerIdx
                    }

                    override fun onPageScrollStateChanged(state: Int) {
                        if (state == ViewPager.SCROLL_STATE_IDLE) {
                            if (!isAutoChangeJobStarted) {
                                autoChangeJobStart()
                            }
                        } else {
                            isAutoChangeJobStarted = false
                            autoChangeJob?.cancel()
                        }
                    }

                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                    }
                })
            }
            autoChangeJobStart()
        } catch (u: UnknownHostException) {
            showOfflineView()
        } catch (e: ConnectException) {
            showOfflineView()
        } catch (te: SocketTimeoutException) {
            showError()
        } catch (e: Errors) {
            when (e) {
                is Errors.OfflineException -> showOfflineView()
                is Errors.FetchException -> showError()
            }
        }
    }

    override fun onDetach() {
        coroutineContext.cancelChildren()
        retrySnack?.dismiss()
        super.onDetach()
    }

    inner class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {


        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.left = space
            outRect.right = space
            outRect.bottom = space

            if (parent.getChildLayoutPosition(view) % 2 == 0) {
                outRect.right = space
            } else {
                outRect.left = space
            }
        }
    }
}
