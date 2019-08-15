package io.dagger.hackernews.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "items")
data class Item(
    val by: String,
    @PrimaryKey
    val id: Long,
    val kids: ArrayList<Long>?,
    val parts: ArrayList<Long>?,
    val parent: Long,
    val descendants: Int,
    val score: Int,
    val time: Long,
    val title: String?,
    val text:String?,
    val type: String,
    val url: String?
) : Serializable {
    val domain: String
        get() {
            val regex = Regex("http(|s)://.+\\.+\\w+/")
            url?.let {
                val match = regex.find(it)
                return match?.value?.removePrefix("https://")?.removePrefix("http://") ?: "nill"
            }
            return "nill"
        }
}