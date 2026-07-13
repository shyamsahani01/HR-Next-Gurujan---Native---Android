package com.example.hrnext.network

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    /** Normalizes a user-entered site URL (may be missing scheme/trailing slash) into a valid base URL. */
    fun normalizeSiteUrl(input: String): String {
        var url = input.trim().trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return "$url/"
    }

    fun build(siteUrl: String, cookieJar: CookieJar): FrappeApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeSiteUrl(siteUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrappeApi::class.java)
    }
}
