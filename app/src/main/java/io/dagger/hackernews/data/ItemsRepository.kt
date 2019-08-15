package io.dagger.hackernews.data

import androidx.annotation.WorkerThread
import io.dagger.hackernews.data.database.ItemsDao
import io.dagger.hackernews.data.model.Item

class ItemsRepository(private val itemsDao: ItemsDao) {

   val savedStories = itemsDao.getSavedStories()

    @WorkerThread
    suspend fun insertStory(item: Item) {
        itemsDao.insertStory(item)
    }

    @WorkerThread
    suspend fun deleteStory(item: Item) {
        itemsDao.deleteStory(item)
    }

    @WorkerThread
    suspend fun getItemId(id:Long):Long? = itemsDao.getItemId(id)
}