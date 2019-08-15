package io.dagger.hackernews.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {

    @TypeConverter
    fun listToJson(value: ArrayList<Long>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToList(value: String): ArrayList<Long>? {
        val listType: Type = object :TypeToken<ArrayList<Long>?>(){}.type
        return Gson().fromJson(value,listType)
    }
}