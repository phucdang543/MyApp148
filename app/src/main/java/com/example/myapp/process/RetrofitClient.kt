package com.example.myapp.process

import android.content.Context
import com.example.myapp.process.favorite.FavoriteApiService
import com.example.myapp.process.getsong.ApiService
import com.example.myapp.process.history.HistoryApiService
import com.example.myapp.process.login.AuthApiService
import com.example.myapp.process.playlist.PlaylistApiService
import com.example.myapp.process.user.UserApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://3.1.202.198:8081/"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor = Interceptor { chain ->
        val prefs = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        HttpLoggingInterceptor.Level.BODY

    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val playlistService: PlaylistApiService by lazy {
        retrofit.create(PlaylistApiService::class.java)
    }

    val favoriteService: FavoriteApiService by lazy {
        retrofit.create(FavoriteApiService::class.java)
    }

    val historyService: HistoryApiService by lazy {
        retrofit.create(HistoryApiService::class.java)
    }

    val userService: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }
}