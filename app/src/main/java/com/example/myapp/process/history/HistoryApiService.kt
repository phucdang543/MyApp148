package com.example.myapp.process.history

import retrofit2.http.GET
import retrofit2.http.Query

interface HistoryApiService {

    @GET("api/song/history")
    suspend fun getPlaybackHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): HistoryResponse
}