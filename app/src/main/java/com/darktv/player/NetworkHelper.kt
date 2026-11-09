package com.darktv.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkHelper {
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val modified = if (request.url.host == "46.245.122.7" && request.header("Referer") == null) {
                request.newBuilder().header("Referer", "https://mytvsat.tv/")
                    .header("Origin", "https://mytvsat.tv").build()
            } else request
            chain.proceed(modified)
        }.connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).followRedirects(true).build()

    @OptIn(UnstableApi::class)
    fun createDataSourceFactory(headers: Map<String, String> = emptyMap()): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(client)
            .setUserAgent(headers["User-Agent"] ?: "Mozilla/5.0 DarkTV/1.0")
            .setDefaultRequestProperties(headers.filterKeys { it != "User-Agent" })
    }
}
