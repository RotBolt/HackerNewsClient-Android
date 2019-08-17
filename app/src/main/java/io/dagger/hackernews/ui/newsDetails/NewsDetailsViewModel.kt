package io.dagger.hackernews.ui.newsDetails

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.dagger.hackernews.utils.Errors
import io.dagger.hackernews.data.ItemsRepository
import io.dagger.hackernews.data.database.ItemDatabase
import io.dagger.hackernews.data.model.CommentItem
import io.dagger.hackernews.data.model.Item
import io.dagger.hackernews.data.remote.HNApiClient
import io.dagger.hackernews.utils.isConnected
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NewsDetailsViewModel(
    application: Application
) : AndroidViewModel(application) {


    private var start = 0
    private val batchSize = 10
    private var end = 10

    val isLoading = MutableLiveData(false)

    var scrollY = 0
    var scrollX = 0

    private val client = HNApiClient(application).hnApiService
    private val commentItemList = mutableListOf<CommentItem>()

    private val itemsRepository: ItemsRepository

    lateinit var commentIds: List<Long>

    init {
        val itemDao = ItemDatabase.getDatabase(application).itemsDao()
        itemsRepository = ItemsRepository(itemDao)
    }


    fun insertItem(item: Item) = viewModelScope.launch {
        itemsRepository.insertStory(item)
    }

    fun deleteItem(item: Item) = viewModelScope.launch {
        itemsRepository.deleteStory(item)
    }

    fun getItemIdAsync(id: Long) = viewModelScope.async {
        itemsRepository.getItemId(id)
    }


    fun getCommentsAsync(isRefresh: Boolean): Deferred<List<CommentItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            if (commentItemList.isNotEmpty() && !isRefresh)
                return@async commentItemList
            if (isConnected(getApplication())) {
                end = if (commentIds.size > batchSize) batchSize else commentIds.size - 1
                val list = getCommentItemBatchAsync(IntRange(start, end), commentIds).await()
                commentItemList.addAll(list)
                commentItemList
            } else {
                throw Errors.OfflineException()
            }
        }
    }

    private fun getCommentItemBatchAsync(
        range: IntRange,
        commentIds: List<Long>
    ): Deferred<List<CommentItem>> {
        return viewModelScope.async {
            val list = mutableListOf<CommentItem>()
            for (i in range) {
                val childItem = client.getItem(commentIds[i]).body()
                childItem?.let {
                    if (it.text != null)
                        list.add(CommentItem(it, null))
                }
            }
            list
        }
    }

    fun getChildCommentsAsync(commentItem: CommentItem): Deferred<List<CommentItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            commentItem.child?.let { return@async it }
            if (isConnected(getApplication())) {
                val childIds = commentItem.item.kids
                if (childIds != null) {
                    val childComments = getCommentItemBatchAsync(IntRange(0, childIds.size - 1), childIds).await()
                    Log.i("PUI", "child ${childComments.size}")
                    commentItem.child = childComments
                    childComments
                } else {
                    throw Errors.FetchException("No Child Comments")
                }
            } else {
                throw Errors.OfflineException()
            }
        }
    }

    fun getMoreCommentsAsync(): Deferred<List<CommentItem>> {
        return viewModelScope.async(Dispatchers.IO) {
            if (end < commentIds.size - 1) {
                start = end + 1
                end = if (commentIds.size - 1 > end + batchSize) end + batchSize else commentIds.size - 1
                val list = getCommentItemBatchAsync(IntRange(start, end), commentIds).await()
                commentItemList.addAll(list)
                commentItemList
            } else {
                emptyList<CommentItem>()
            }
        }
    }

    fun isNotFullLoaded(): Boolean = end < commentIds.size - 1

}