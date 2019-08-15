package io.dagger.hackernews.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.dagger.hackernews.utils.Errors
import io.dagger.hackernews.data.model.Item
import io.dagger.hackernews.data.remote.HNApiClient
import io.dagger.hackernews.utils.getSafeResponse
import io.dagger.hackernews.utils.isConnected
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DashNewsViewModel(application: Application) : AndroidViewModel(application) {
    private val client = HNApiClient(application).hnApiService

    private val topItems = mutableListOf<Item>()
    private val newItems= mutableListOf<Item>()
    private val showItems= mutableListOf<Item>()
    private val jobItems = mutableListOf<Item>()
    private val askItems = mutableListOf<Item>()


    fun geItemsAsync(type: String, isRefresh: Boolean): Deferred<List<Item>> {
        return viewModelScope.async(Dispatchers.IO) {
            val list = getListCategory(type)
            if (list.isNotEmpty() && !isRefresh){
                return@async list
            }else{
                if (isConnected(getApplication())){
                    val itemIds = getItemIds(type)
                    val itemList = getItemList(itemIds,type == "Top")
                    list.apply {
                        clear()
                        addAll(itemList)
                    }
                    list
                }else{
                    throw Errors.OfflineException()
                }
            }
        }
    }

    private fun getListCategory(type: String) = when (type) {
        "Top" -> topItems
        "New" -> newItems
        "Ask" -> askItems
        "Job" -> jobItems
        "Show" -> showItems
        else -> throw Errors.UnknownCategory()

    }

    private suspend fun getItemIds(type: String)=when(type){
        "Top" -> getSafeResponse(client.topStories())
        "New" -> getSafeResponse(client.newStories())
        "Ask" -> getSafeResponse(client.askStories())
        "Job" -> getSafeResponse(client.jobStories())
        "Show" -> getSafeResponse(client.showStories())
         else -> throw Errors.UnknownCategory()
    }


    private suspend fun getItemList(list: List<Long>, isBanner: Boolean = false): List<Item> {
        return if (list.isNotEmpty()) {
            val end = if (isBanner) 6 else 4
            val itemList = mutableListOf<Item>()
            for (i in 0 until end) {
                val item = client.getItem(list[i]).body()
                item?.let { itemList.add(item) }
            }
            itemList
        } else {
            emptyList()
        }
    }


}