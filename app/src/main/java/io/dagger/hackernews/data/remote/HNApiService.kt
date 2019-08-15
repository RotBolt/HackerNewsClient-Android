package io.dagger.hackernews.data.remote

import io.dagger.hackernews.data.model.Item
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface HNApiService {

    @GET("/v0/item/{id}.json")
    suspend fun getItem(@Path("id") id: Long): Response<Item>

    @GET("/v0/topstories.json")
    suspend fun topStories(): Response<List<Long>>

    @GET("/v0/newstories.json")
    suspend fun newStories(): Response<List<Long>>

    @GET("/v0/askstories.json")
    suspend fun askStories(): Response<List<Long>>

    @GET("/v0/showstories.json")
    suspend fun showStories(): Response<List<Long>>

    @GET("/v0/jobstories.json")
    suspend fun jobStories(): Response<List<Long>>
}