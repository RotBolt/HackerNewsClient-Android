package io.dagger.hackernews.data.remote

import android.content.Context
import io.dagger.hackernews.utils.isConnected
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class HNApiClient(private val context: Context) {

    private val HN_BASE_URl = "https://hacker-news.firebaseio.com"


    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(provideHttpLoggingInterceptor())
            .addNetworkInterceptor(provideCacheInterceptor())
            .addInterceptor(provideOfflineCacheInterceptor())
            .cache(getCache())
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()


    private val retrofit =
        Retrofit.Builder()
            .baseUrl(HN_BASE_URl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val hnApiService: HNApiService =
        retrofit.create(HNApiService::class.java)


    private fun provideCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val maxAge = 60 // read from cache for 60 seconds even if there is internet connection
            response.newBuilder()
                .header("Cache-Control", "public, max-age=$maxAge")
                .removeHeader("Pragma")
                .build()
        }
    }

    private fun provideOfflineCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            if (!isConnected(context)) {
                val maxStale = 60 * 60 * 5
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                    .removeHeader("Pragma")
                    .build()
            }
            chain.proceed(request)
        }
    }

    private fun provideHttpLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
    }


    private fun getCache(): Cache {
        val cacheSize = (10 * 1024 * 1024).toLong()
        return Cache(context.cacheDir, cacheSize)
    }
}



