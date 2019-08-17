package io.dagger.hackernews.ui.newsType

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.dagger.hackernews.data.ItemsRepository
import io.dagger.hackernews.data.database.ItemDatabase
import io.dagger.hackernews.data.model.Item
import io.dagger.hackernews.data.remote.HNApiClient
import io.dagger.hackernews.utils.Errors
import io.dagger.hackernews.utils.getSafeResponse
import io.dagger.hackernews.utils.isConnected
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class NewsTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val itemsRepository: ItemsRepository
    private val client = HNApiClient(application).hnApiService

    private var typeStories = mutableListOf<Item?>()
    private var itemIds: List<Long> = emptyList()
    private val mutableListItems = mutableListOf<Item?>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    var scrollPosition = 0

    private val batchSize = 10
    private var start = 0
    private var end = batchSize

    init {
        val itemDao = ItemDatabase.getDatabase(application).itemsDao()
        itemsRepository = ItemsRepository(itemDao)
    }

    val savedStories by lazy {
        itemsRepository.savedStories
    }

    fun getStoriesAsync(type: String, isRefresh: Boolean): Deferred<List<Item?>> {
        return viewModelScope.async {
            if (typeStories.isNotEmpty() && !isRefresh) {
                return@async typeStories
            } else {
                if (isConnected(getApplication())) {
                    itemIds = getItemIds(type)
                    end = if (batchSize >= itemIds.size) itemIds.size - 1 else batchSize
                    val itemList = getItemListBatchAsync(IntRange(start, end)).await()
                    mutableListItems.addAll(itemList)
                    typeStories = mutableListItems
                    typeStories
                } else {
                    throw Errors.OfflineException()
                }
            }
        }
    }


    private suspend fun getItemIds(newsType: String): List<Long> {
        return when (newsType) {
            "Top" -> getSafeResponse(client.topStories())
            "New" -> getSafeResponse(client.newStories())
            "Ask" -> getSafeResponse(client.askStories())
            "Job" -> getSafeResponse(client.jobStories())
            "Show" -> getSafeResponse(client.showStories())
            else -> throw Errors.UnknownCategory()
        }
    }


    private fun getItemListBatchAsync(range: IntRange): Deferred<List<Item>> {
        return viewModelScope.async(Dispatchers.IO) {
            val itemList = mutableListOf<Item>()

            if (itemIds.isNotEmpty()) {
                for (i in range) {
                    val id = itemIds[i]
                    val item = client.getItem(id).body()
                    item?.let { itemList.add(it) }
                }
            }

            itemList
        }
    }

    fun loadMoreItemsAsync(): Deferred<List<Item>> {
        return viewModelScope.async {
            if (end < itemIds.size - 1) {
                start = end + 1
                end =
                    if (end + batchSize < itemIds.size) end + batchSize
                    else itemIds.size - 1

                val itemList = getItemListBatchAsync(IntRange(start, end)).await()
                mutableListItems.addAll(itemList)
                typeStories = mutableListItems
                return@async itemList
            } else {
                return@async emptyList<Item>()
            }
        }
    }

    fun isNotFullLoaded(): Boolean =
        if (itemIds.isNotEmpty())
            end < itemIds.size - 1
        else
            false

}